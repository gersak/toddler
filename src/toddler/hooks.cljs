(ns toddler.hooks
  (:require
    [clojure.set]
    [goog.string :as gstr]
    [goog.string.format]
    [clojure.core.async :as async :refer-macros [go-loop]]
    [helix.core :refer-macros [defhook]]
    [helix.hooks :as hooks]
    [toddler.app :as app]
    [toddler.util :as util]
    [toddler.i18n :as i18n :refer [translate]]
    [toddler.i18n.keyword]
    [toddler.i18n.time]
    [toddler.i18n.number]
    [toddler.graphql :as graphql]))


(.log js/console "Loading toddler.hooks")


(defhook use-url
  "Returns root application root URL"
  []
  (hooks/use-context app/url))

(defhook use-graphql-url
  "Returns GraphQL endpoint URL"
  []
  (let [root (use-url)
        from-context (hooks/use-context app/graphql-url)]
    (hooks/use-memo
      [root from-context]
      (or from-context (str root "/graphql")))))


(defhook use-user
  "Returns value in app/*user* context"
  []
  (hooks/use-context app/user))


(defhook use-token
  "Returns current app token if any."
  []
  (hooks/use-context app/token))


(defhook use-local-storage
  ([location] (use-local-storage location identity))
  ([location transform]
    (let [[local set-local!] (hooks/use-state
                               (transform
                                 (.getItem js/localStorage location)))]
      (hooks/use-effect
        [local]
        (if (some? local)
          (.setItem js/localStorage location local)
          (.removeItem js/localStorage location)))
      [local set-local!])))


(defhook use-avatar
  [{:keys [name avatar path]}]
  (let [avatars (hooks/use-context app/avatars)
        [_avatar set-avatar!] (hooks/use-state (get @avatars avatar))
        [token] (hooks/use-context app/token)
        refresh (hooks/use-callback
                  [_avatar avatar path]
                  (fn []
                    (when avatar
                      (let [xhr (new js/XMLHttpRequest)]
                        (.open xhr "GET" path true)
                        (when token (.setRequestHeader xhr "Authorization" (str "Bearer " token)))
                        (.setRequestHeader xhr "Accept" "application/octet-stream")
                        (.setRequestHeader xhr "Cache-Control" "no-cache")
                        (.addEventListener
                          xhr "load"
                          (fn [evt]
                            (let [status (.. evt -target -status)
                                  avatar' (.. evt -currentTarget -responseText)]
                              (case status
                                200
                                (cond
                                  ;; if avatar has changed than swap avatars
                                  ;; this should trigger updates for all hooks
                                  ;; with target avatar
                                  (not= avatar' (get @avatars avatar))
                                  (swap! avatars assoc avatar avatar')
                                  ;; Otherwise if avatar is cached properly, but
                                  ;; current _avatar doesn't match current state
                                  ;; update current _avatar
                                  (not= _avatar avatar')
                                  (set-avatar! avatar'))
                                ;; otherwise
                                nil
                                (async/put! app/signal-channel
                                  {:type :toddler.notifications/error
                                   :message (str "Couldn't fetch avatar for user " name)
                                   :visible? true
                                   :hideable? true
                                   :adding? true
                                   :autohide true})))))
                        (.send xhr)))))]
    (hooks/use-effect
      [avatar]
      (when (some? avatar)
        (if-let [cached (get @avatars avatar)]
          (set-avatar! cached)
          (refresh))))
    (hooks/use-effect
      [avatar]
      (let [uuid (random-uuid)]
        (when (and avatars avatar)
          (add-watch avatars uuid
                     (fn [_ _ o n]
                       (let [old (get o avatar)
                             new (get n avatar)]
                         (when (not= old new)
                           (set-avatar! new))))))
        (fn []
          (when avatars (remove-watch avatars uuid)))))
    [_avatar refresh]))


(defhook use-current-locale
  "Returns value for :locale in current user settings"
  []
  (let [[{{locale :locale
           :or {locale :default}} :settings}] (use-user)]
    locale))


(defhook use-translate
  []
  (let [locale (use-current-locale)
        translate (hooks/use-memo
                    [locale]
                    (fn
                      ([data] (translate data locale))
                      ([data options] (translate data locale options))))]
    translate))


(comment
  (apply gstr/format
         (translate :reacher.delete.dialog :hr)
         ["Biljana"]))


(defhook use-translatef
  []
  (let [locale (use-current-locale)
        translate (hooks/use-memo
                   [locale]
                   (fn
                     ([data & args]
                       (if-let [template (translate data locale)]
                         (apply gstr/format template args)
                         (throw (js/Error. (str "Couldn't find translation for " data ". Locale: " locale)))))))]
    translate))


(defhook use-calendar
  [key]
  (let [locale (use-current-locale)]
    (hooks/use-memo
     [locale]
     (i18n/locale locale key))))


(defn make-idle-service
  "Creates idle service that will return idle-channel. This channel can be used
  to async/put! values in channel.
  
  Service accepts period and function. When idle-channel hasn't received any data for
  period of time, than input function is called on last recevied value."
  ([period f]
   (assert (and (number? period) (pos? period)) "Timeout period should be positive number.")
   (assert (fn? f) "Function not provided. No point if no action is taken on idle timeout.")
   (let [idle-channel (async/chan (async/sliding-buffer 2))]
     ;; When some change happend
     (async/go-loop [v (async/<! idle-channel)]
       (if (nil? v)
         :IDLED
         ;; If not nil new value received and now idle handling should begin
         (let [aggregated-values
               (loop [[value _]
                      (async/alts!
                       [idle-channel (async/go (async/<! (async/timeout period))
                                               ::TIMEOUT)])
                      r [v]]
                 (if (or
                      (= ::TIMEOUT value)
                      (nil? value))
                   (conj r value)
                   (recur (async/alts! [idle-channel (async/go (async/<! (async/timeout period)) ::TIMEOUT)]) (conj r value))))]
           ;; Apply function and if needed recur
           (f aggregated-values)
           (if (nil? (last aggregated-values))
             nil
             (recur (async/<! idle-channel))))))
     idle-channel)))


(defhook use-idle
  "Idle hook. Returns cached value and update fn. Input arguments
  are initial state, callback that should will be called on idle
  timeout."
  ([state callback] (use-idle state callback 500))
  ([state callback timeout]
   (assert (fn? callback) "Callback should be function")
   (let [[v u] (hooks/use-state state)
         call (hooks/use-ref callback)
         initialized? (hooks/use-ref false)
         idle-channel (hooks/use-ref nil)]
     ;; Create idle channel
     (hooks/use-effect
      :once
      (reset!
       idle-channel
       (make-idle-service
        timeout
        (fn [values]
          (let [v' (last (butlast values))]
            (if @initialized?
              (when (ifn? @call) (@call v'))
              (reset! initialized? true))))))
      (fn []
        (when @idle-channel (async/close! @idle-channel))))
     ;; When callback is changed reference new callback
     (hooks/use-effect
      [callback]
      (reset! call callback))
     ;; When value has changed and there is idle channel
     ;; put new value to idle-channel
     (hooks/use-effect
      [v]
      (when @idle-channel
        (async/put! @idle-channel (or v :NULL))))
     ;; Return local state and update fn
     [v u])))


(defhook use-delayed
  "Function returns `stable` input state after timeout. Idle service
  is created that tracks input state and when this state is not changed
  after timeout, than return state is updated.
  
  Update will trigger react component rendering same as use-state"
  ([state] (use-delayed state 500))
  ([state timeout]
   (let [current-value (hooks/use-ref state)
         [v u] (hooks/use-state state)
         idle-channel (hooks/use-ref nil)]
     (hooks/use-effect
      :once
      (reset!
       idle-channel
       (make-idle-service
        timeout
        (fn [values]
          (let [v (last (butlast values))
                v (if (= v ::NULL) nil v)]
            (when (not= @current-value v)
              (reset! current-value v)
              (u v))))))
      (fn []
        (when @idle-channel (async/close! @idle-channel))))
     (hooks/use-effect
      [state]
      (when @idle-channel
        (async/put! @idle-channel (or state ::NULL))))
     v)))


(defhook use-window-dimensions
  "Function will return browser window dimensions that
  should be instantiated in app/*window* context"
  []
  (hooks/use-context app/window))



(defhook use-dimensions
  "Hook returns ref that should be attached to component and
  second result dimensions of bounding client rect"
  ([]
    (let [node (hooks/use-ref nil)
          observer (hooks/use-ref nil)
          [dimensions set-dimensions!] (hooks/use-state nil)
          resize-idle-service (hooks/use-ref
                                (make-idle-service
                                  50
                                  (fn handle [entries]
                                    (let [[_ entry] (reverse entries)
                                          content-rect (.-contentRect entry)
                                          dimensions {:width (.-width content-rect)
                                                      :height (.-height content-rect)
                                                      :top (.-top content-rect)
                                                      :left (.-left content-rect)
                                                      :right (.-right content-rect)
                                                      :bottom (.-bottom content-rect)
                                                      :x (.-x content-rect)
                                                      :y (.-y content-rect)}]
                                      (set-dimensions! dimensions)))))]
      (hooks/use-effect
        :always
        (when (and (some? @node) (nil? dimensions))
          (letfn [(reset [[entry]]
                    (async/put! @resize-idle-service entry))]
            (reset! observer (js/ResizeObserver. reset))
            (.observe @observer @node)
            (set-dimensions! (util/bounding-client-rect @node))
            nil)))
      (hooks/use-effect
        :once
        (fn [] (when @observer (.disconnect @observer))))
      [node dimensions]))
  ([ks]
    (let [nodes (hooks/use-ref nil)
          refs (hooks/use-memo
                 [ks]
                 (reduce
                   (fn [r k]
                     (assoc r k (fn [node]
                                  (swap! nodes assoc k node))))
                   nil
                   ks))
          observers (hooks/use-ref nil)
          [dimensions set-dimensions!] (hooks/use-state nil)]
      ;; Always check if everything is observedd!
      (hooks/use-effect
        :always
        (doseq [k ks
                :let [observer (get @observers k)
                      node (get @nodes k)]
                :when (and node (nil? observer))]
          (letfn [(reset [[entry]]
                    (let [content-rect (.-contentRect entry)]
                      (set-dimensions! assoc k
                                       {:width (.-width content-rect)
                                        :height (.-height content-rect)
                                        :top (.-top content-rect)
                                        :left (.-left content-rect)
                                        :right (.-right content-rect)
                                        :bottom (.-bottom content-rect)
                                        :x (.-x content-rect)
                                        :y (.-y content-rect)})))]
            (swap! observers assoc k (js/ResizeObserver. reset))
            (.observe (get @observers k) node))))
      ;; Register on remove cleanup
      (hooks/use-effect
        :once
        (fn []
          (doseq [k ks
                  :let [observer (get @observers k)]
                  :when observer]
            (.disconnect observer))))
      [refs dimensions])))


(defhook use-parent [_ref]
  (util/dom-parent _ref))


(defhook use-on-parent-resized [_ref handler]
  (let [observer (hooks/use-ref nil)
        resize-idle-service (hooks/use-ref
                              (make-idle-service
                                300
                                (fn handle [entries]
                                  (when (ifn? handler)
                                    (let [[_ entry] (reverse entries)
                                          content-rect (.-contentRect entry)
                                          dimensions {:width (.-width content-rect)
                                                      :height (.-height content-rect)
                                                      :top (.-top content-rect)
                                                      :left (.-left content-rect)
                                                      :right (.-right content-rect)
                                                      :bottom (.-bottom content-rect)
                                                      :x (.-x content-rect)
                                                      :y (.-y content-rect)}]
                                      (handler dimensions (.-target entry)))))))]
    (hooks/use-effect
      :always
      (when-not @observer
        (when @_ref
          (when-let [parent (util/dom-parent @_ref)]
            (letfn [(resized [[entry]]
                      (async/put! @resize-idle-service entry))]
              (reset! observer (js/ResizeObserver. resized))
              (.observe @observer parent))))))
    (hooks/use-effect
      :once
      #_(when-let [parent (util/dom-parent @_ref)]
        (when (ifn? handler)
          (handler (util/bounding-client-rect parent) parent)))
      (fn [] (when @observer (.disconnect @observer))))))


(defhook use-publisher
  "Generic publisher function. Provide topic fn
  and `use-publisher` will return [publisher publish] 
  
  Publisher is async channel that publishes messages, and
  publish is function that accepts single [data] argument.

  This data is published over publisher.
  
  `publisher` should be used with `use-listener`"
  ([topic-fn] (use-publisher topic-fn 5000))
  ([topic-fn buffer-size]
   (let [[pc set-pc] (hooks/use-state nil)
         [publisher set-publisher] (hooks/use-state nil)
         publish (hooks/use-memo
                  [publisher]
                  (fn [data]
                    (when pc (async/put! pc data))))]
     (hooks/use-effect
      :once
      (let [pc' (async/chan buffer-size)
            p (async/pub pc' topic-fn)]
        (set-pc pc')
        (set-publisher p))
      #(do
         (when pc (async/close! pc))))
     [publisher publish])))


(defhook use-listener
  "Generic listener hook. For given publisher and topic apply handler."
  [publisher topic handler]
  (hooks/use-effect
   [publisher handler]
   (when publisher
     (let [c (async/chan 100)]
       (async/sub publisher topic c)
       (go-loop []
         (let [v (async/<! c)]
           (when v
             (handler v)
             (recur))))
       #(when publisher
          (async/unsub publisher topic c)
          (async/close! c))))))



(defhook use-toddler-listener
  "Function will register handler for topic on global
  toddler toddler.app/signal-publisher"
  [topic handler]
  (hooks/use-effect
   :once
   (let [c (async/chan 10)]
     (async/sub app/signal-publisher topic c)
     (async/go-loop []
       (let [v (async/<! c)]
         (when v
           (handler v)
           (recur))))
     (fn [] (async/close! c)))))


(defhook use-toddler-publisher
  "Function will forward all input data to global
  toddler.app/signal-channel. Data will be published
  through toddler.app/signal-publisher.
  
  See `use-toddler-listener` for handling published signals"
  []
  (let [publisher (hooks/use-memo
                   :once
                   (fn [data]
                     (async/put! app/signal-channel data)))]
    publisher))


(defhook use-query
  ([{query-name :query
     selection :selection
     args :args
     :keys [on-loaded on-error]}]
    (let [[token] (use-token)
          url (use-graphql-url)]
      (hooks/use-memo
        [(name query-name) args selection]
        (fn send
          []
          (let [query-name (name query-name)
                query-key (keyword query-name)
                query (graphql/wrap-queries
                        (graphql/->graphql 
                          (graphql/->GraphQLQuery
                            query-name nil selection args)))]
            (async/go
              (let [{:keys [errors]
                     {data query-key} :data
                     :as response}
                    (async/<! 
                      (graphql/send-query 
                        query 
                        :url url
                        :token token
                        :on-loaded on-loaded 
                        :on-error on-error))]
                (if (some? errors)
                  (ex-info "Remote query failed"
                           {:query query
                            :args args
                            :selection selection
                            :response response})
                  data)))))))))


(defhook use-queries
  ([queries] (use-queries queries nil))
  ([queries {:keys [on-loaded on-error]}]
    (let [[token] (use-token)
          url (use-graphql-url)]
      (hooks/use-memo
        [queries]
        (fn send
          []
          (let [
                query (apply
                        graphql/wrap-queries
                        (map
                          (fn [{query-name :query
                                selection :selection
                                args :args}]
                            (let [query-name (name query-name)]
                              (graphql/->graphql 
                                (graphql/->GraphQLQuery
                                  query-name nil selection args))))
                          queries))]
            (async/go
              (let [{:keys [errors]
                     data :data
                     :as response}
                    (async/<! 
                      (graphql/send-query 
                        query 
                        :url url
                        :token token
                        :on-loaded on-loaded 
                        :on-error on-error))]
                (if (some? errors)
                  (ex-info "Remote query failed"
                           {:queries queries
                            :response response})
                  data)))))))))


(defhook use-mutation
  ([{:keys [mutation selection variables]
     {:keys [on-loaded on-error]} :handlers}]
    (let [[token] (use-token)
          [mutation-fragment type-declarations variable-mapping]
          (hooks/use-memo
            [mutation variables selection]
            (graphql/gen-mutation mutation variables selection))
          ;;
          url (use-graphql-url)]
      (hooks/use-memo
        [mutation-fragment]
        (fn [data]
          (async/go
            (let [query (binding [toddler.graphql/*variable-bindings* type-declarations]
                          (graphql/wrap-mutations mutation-fragment))
                  {:keys [errors]
                   {data mutation} :data}
                  (async/<! 
                    (graphql/send-query 
                      query 
                      :url url
                      :token token
                      :variables (clojure.set/rename-keys data variable-mapping)
                      :on-load on-loaded
                      :on-error on-error))]
              (if (some? errors)
                (ex-info
                  (str "Mutation " mutation " failed")
                  {:query query
                   :variables variables
                   :errors errors})
                data))))))))
