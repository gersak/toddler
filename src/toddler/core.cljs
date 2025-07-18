(ns toddler.core
  (:require-macros [toddler.core :refer [mlf]])
  (:require
   ["react-dom" :as rdom]
   [shadow.loader]
   [clojure.set]
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [goog.string :as gstr]
   [goog.string.format]
   [clojure.core.async :as async :refer-macros [go-loop]]
   [helix.core :refer-macros [defnc defhook fnc $ provider]]
   [helix.hooks :as hooks]
   [helix.children :refer [children]]
   [toddler.app :as app]
   [toddler.util :as util]
   [toddler.i18n :as i18n :refer [translate]]
   [toddler.i18n.keyword]
   [toddler.i18n.time]
   [toddler.i18n.number]
   [toddler.graphql :as graphql]
   [toddler.graphql.transport :refer [send-query]]))

(defonce mouse-position (atom nil))

(defn ml
  "Multiline function. Joins input lines"
  [& lines]
  (clojure.string/join "\n" lines))

(defn fetch
  "Function will fetch content from URL and return string
  representation"
  [url]
  (let [result (async/promise-chan)]
    (-> (js/fetch url)
        (.then
         (fn [response]
           (if (.-ok response)
             (-> (.text response)
                 (.then (fn [text] (async/put! result text)))
                 (.catch (fn [_] (async/close! result))))
             (do
               (.error js/console (js/Error (str "Failed to fetch: " url)))
               (async/close! result)))))
        (.catch
         (fn [err]
           (.error js/console (str "Failed fetching file: " (pr-str url)) err)
           (async/close! result))))
    result))

(defn conj-prop-classes
  "Utility function that will create vector
  from class and className props
  
  Return vector of strings"
  ([props] (conj-prop-classes nil props))
  ([classes {:keys [class className]}]
   (cond-> (or classes [])
     (string? class) (conj class)
     (string? className) (conj className)
     (sequential? class) (into class))))

(letfn [(target [location]
          (if-some [_ns (try (namespace location) (catch js/Error _ nil))]
            (str _ns \/ (name location))
            (name location)))]
  (defhook use-local-storage
    "For local storage usage. Hook will return local state
    and second argument is set-local! that will store/update values
    in local storage at 'location'
    
    Location can be string, keyword, symbol"
    ([location] (use-local-storage
                 location
                 (fn [v] (when v (edn/read-string v)))))
    ([location transform]
     (let [target (target location)
           [local set-local!] (hooks/use-state
                               (transform
                                (.getItem js/localStorage target)))]
       (hooks/use-effect
         [local]
         (if (some? local)
           (.setItem js/localStorage target local)
           (.removeItem js/localStorage target)))
       [local set-local!]))))

(defhook use-mouse-tracker
  "Hook will add mousemove event on js/window and
  store updates in toddler.core/mouse-position atom."
  []
  (hooks/use-effect
    :once
    (letfn [(track [e]
              (reset! mouse-position
                      {:x (.-clientX e)
                       :y (.-clientY e)}))]
      (.addEventListener js/window "mousemove" track)
      (fn [] (.removeEventListener js/window "mousemove" track)))))

(defhook use-is-touch? []
  (hooks/use-memo
    :once
    (.-matches (.matchMedia js/window "(pointer: coarse)"))))

(defn get-mouse-position
  "Returns current mouse position."
  []
  (deref mouse-position))

(defhook use-mouse-position
  "Hook will return mouse position. If mouse position is changed
  it will change state and cause re-render!"
  ([] (use-mouse-position true))
  ([track?]
   (let [[state set-state!] (hooks/use-state nil)
         k (str (gensym "mouse_position_"))]
     (hooks/use-effect
       [track?]
       (when track?
         (add-watch mouse-position k
                    (fn [_ _ _ s]
                      (set-state! s))))
       (fn []
         (remove-watch mouse-position k)))
     state)))

(defhook use-url
  "Returns application root URL"
  []
  (hooks/use-context app/url))

(defhook use-layout
  "Returns value of application layout context"
  []
  (hooks/use-context app/layout))

(defhook use-theme
  "Returns theme context value"
  []
  (hooks/use-context app/theme))

(defhook use-theme-change
  "Returns theme change function that will set theme state."
  []
  (hooks/use-context app/change-theme))

(defhook use-theme-state
  "Hook will provide theme state and state setter
  similar to helix.hooks/use-state. Except this
  state will be stored in localStorage.
  
  When value of theme changes, it will store that value
  to local storage and check if `<html>` attribute `data-theme`
  has the same value as theme state"
  ([] (use-theme-state ::theme))
  ([localstorage-key] (use-theme-state localstorage-key "light"))
  ([localstorage-key default]
   (let [[theme set-theme!] (use-local-storage localstorage-key str)]
     (hooks/use-effect
       [theme]
       (if (empty? theme)
         (set-theme! default)
         (async/go
           (loop []
             (if-some [html (.querySelector js/document "html")]
               (when-not (= (.getAttribute html "data-theme") theme)
                 (.setAttribute html "data-theme" theme))
               (do
                 (async/<! (async/timeout 100))
                 (recur)))))))
     [theme set-theme!])))

(defn wrap-theme
  "Function will wrap component with provided `toddler.app/theme` and
  `toddler.app/change-theme` context. Theme state will be stored in
  localstorage under `toddler.core/theme` key"
  ([component] (wrap-theme component ::theme))
  ([component key] (wrap-theme component key "light"))
  ([component key default]
   (fnc Theme [props]
     (let [[theme set-theme!] (use-theme-state key default)]
       (provider
        {:context app/theme
         :value theme}
        (provider
         {:context app/change-theme
          :value set-theme!}
         ($ component {& props})))))))

(defhook use-locale
  "Returns theme context value"
  []
  (hooks/use-context app/locale))

(defhook use-locale-change
  "Returns locale change function that will set theme state."
  []
  (hooks/use-context app/change-locale))

(defhook use-locale-state
  "Hook will provide theme state and state setter
  similar to helix.hooks/use-state. Except this
  state will be stored in localStorage.
  
  When value of theme changes, it will store that value
  to local storage and check if `<html>` attribute `data-theme`
  has the same value as theme state"
  ([] (use-locale-state ::locale))
  ([localstorage-key] (use-locale-state localstorage-key nil))
  ([localstorage-key default]
   (let [[locale set-locale!] (use-local-storage
                               localstorage-key
                               (fn [v]
                                 (when v
                                   (try
                                     (keyword (str/lower-case (name (edn/read-string v))))
                                     (catch js/Error _
                                       (.error js/console "Couldn't read locale: " v))))))]
     [locale set-locale!])))

(defn wrap-locale
  "Function will wrap component with provided `toddler.app/locale` and
  `toddler.app/change-locale` context. Locale state will be stored in
  localstorage under `toddler.core/locale` key"
  ([component] (wrap-locale component ::locale))
  ([component key] (wrap-locale component key :default))
  ([component key default]
   (fnc Locale [props]
     (let [[locale set-locale!] (use-locale-state key default)]
       (provider
        {:context app/locale
         :value (or locale :default)}
        (provider
         {:context app/change-locale
          :value set-locale!}
         ($ component {& props})))))))

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

(letfn [(target [location]
          (if-some [_ns (try (namespace location) (catch js/Error _ nil))]
            (str _ns \/ (name location))
            (name location)))]
  (defhook use-session-storage
    "For session storage usage. Hook will return session storage state
    and second argument is set-local! that will store/update values
    in session storage at 'location'

    Location can be string, keyword, symbol"

    ([location] (use-session-storage
                 location
                 (fn [v]
                   (when v (edn/read-string v)))))
    ([location transform]
     (let [target (target location)
           [local set-local!] (hooks/use-state
                               (transform
                                (.getItem js/sessionStorage target)))]
       (hooks/use-effect
         [local]
         (if (some? local)
           (.setItem js/sessionStorage target local)
           (.removeItem js/sessionStorage target)))
       [local set-local!]))))

(letfn [(target [location]
          (if-some [_ns (try (namespace location) (catch js/Error _ nil))]
            (str _ns \/ (name location))
            (name location)))]
  (defhook use-session-cache
    "Hook that will store variable value in browser session storage when ever
    value changes. If init-fn is provided it will be called on last recorded
    value for given variable that is found under location key in browser session
    storage.
    
    Returns atom"
    ([location value]
     (use-session-cache
      location
      (fn [v] (when v (edn/read-string v)))
      value))
    ([location transform value]
     (use-session-cache
      location
      transform
      value
      nil))
    ([location transform value init-fn]
     (let [target (target location)
           initialized? (hooks/use-ref false)
           _ref (hooks/use-ref
                 (when-let [data (.getItem js/sessionStorage target)]
                   (transform data)))]
       (hooks/use-effect
         [value]
         (when @initialized?
           (if (some? value)
             (.setItem js/sessionStorage target value)
             (.removeItem js/sessionStorage target))
           (reset! _ref value)))
       (hooks/use-effect
         :once
         (when (ifn? init-fn)
           (if-let [data (.getItem js/sessionStorage target)]
             (init-fn (transform data))
             (init-fn nil)))
         (reset! initialized? true))
       _ref))))

(defhook use-translate
  "Hook will return function that when called will based
  on toddler.app/locale context translate input value.
  
  Supported translation values are number,Date,keyword and UUID"
  []
  (let [locale (use-locale)
        translate (hooks/use-memo
                    [locale]
                    (fn
                      ([data] (translate data locale))
                      ([data options]
                       (if (number? data)
                         (translate data options)
                         (translate data locale options)))))]
    translate))

(defhook use-translatef
  "Hook will return function that when called will based
  on toddler.app/locale context translate input value.

  Supported translation values are number,Date,keyword and UUID"
  []
  (let [locale (use-locale)
        translate (hooks/use-memo
                    [locale]
                    (fn
                      ([data & args]
                       (if-let [template (translate data locale)]
                         (try
                           (apply gstr/format template args)
                           (catch js/Error _
                             (let [message
                                   (str "Couldn't translate " data
                                        "\n"
                                        (with-out-str
                                          (pprint
                                           {:args args
                                            :template template})))]
                               (.error js/console message)
                               "")))
                         (throw (js/Error. (str "Couldn't find translation for " data ". Locale: " locale)))))))]
    translate))

(defhook use-calendar
  "Hook will return values of current locale
  for key:

     * :months
     * :months/standalone
     * :months/short
     * :months.standalone/short
     * :eras
     * :era/names
     * :months/narrow
     * :weekdays
     * :weekdays/standalone
     * :weekdays/short
     * :weekdays.standalone/short
     * :weekdays/narrow
     * :weekdays.standalone/narrow
     * :quarters
     * :quarters/short
     * :ampms
     * :weekends
     * :weekdays/first"
  [key]
  (let [locale (use-locale)]
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
     (async/go
       (loop [v (async/<! idle-channel)]
         (if (nil? v)
           :IDLED
           ;; If not nil new value received and now idle handling should begin
           (let [aggregated-values; [v]
                 (loop [[value _] (async/alts!
                                   [idle-channel
                                    (async/go
                                      (async/<! (async/timeout period))
                                      ::TIMEOUT)])
                        r [v]]
                   (if (or
                        (= ::TIMEOUT value)
                        (nil? value))
                     ;; Return aggregated values
                     (conj r value)
                     ;; Otherwise wait for next value in idle-channel
                     ;; and recur
                     (recur (async/alts!
                             [idle-channel
                              (async/go
                                (async/<! (async/timeout period))
                                ::TIMEOUT)])
                            (conj r value))))]
             ;; Apply function and if needed recur
             (f aggregated-values)
             (if (nil? (last aggregated-values))
               nil
               (recur (async/<! idle-channel)))))))
     idle-channel)))

(defhook use-idle
  "Idle hook. Returns cached value and update fn. Input arguments
  are initial state, callback that should will be called on idle
  timeout."
  ([state callback] (use-idle state callback {:timeout 500}))
  ([state callback
    {:keys [timeout initialized?]
     :or {timeout 500
          initialized? false}}]
   (assert (fn? callback) "Callback should be function")
   (let [[v u] (hooks/use-state state)
         call (hooks/use-ref callback)
         initialized? (hooks/use-ref initialized?)
         idle-channel (hooks/use-ref nil)]
     ;; Create idle channel
     (hooks/use-effect
       :once
       (reset!
        idle-channel
        (make-idle-service
         timeout
         (fn [values]
           (let [[_ v'] (reverse values)]
             (if @initialized?
               (when (ifn? @call) (@call v'))
               (reset! initialized? true))))))
       (fn []
         (when @idle-channel
           (async/close! @idle-channel))))
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

(defhook use-window-width
  "Returns visible window width size"
  []
  (let [{:keys [width]} (hooks/use-context app/window)]
    width))

(defhook use-window-height
  "Returns visible window height size"
  []
  (let [{:keys [height]} (hooks/use-context app/window)]
    height))

(defhook use-window-width-test
  "Test window width with predicate. Should be used like
  
  `(use-window-width-test < 400)`

  to test if window width is less than 400px"
  [pred size]
  (let [{:keys [width]} (hooks/use-context app/window)]
    (pred width size)))

(defhook use-resize-observer
  "Hook returns ref that should be attached to component and
  second argument is handler that will be called when
  resize is observed with bounding client rect arguments"
  ([node f]
   (let [observer (hooks/use-ref nil)
         current-size (hooks/use-ref nil)]
     (hooks/use-effect
       [node]
       (when (some? node)
         (letfn [(reset [[entry]]
                   (let [entry-target (.-target entry)
                         rect (util/bounding-client-rect entry-target)]
                     (when (not= @current-size rect)
                       (reset! current-size rect)
                       (f rect))))]
           (reset! observer (js/ResizeObserver. reset))
           (.observe @observer node)
           nil))
       (fn [] (when @observer (.disconnect @observer))))
     node)))

(defhook use-dimensions
  "Hook returns ref that should be attached to component and
  second result dimensions of bounding client rect.
  
  You can specify if you wan't to track border box sizing with
  ':box' value or contentRect with 'content' sizing value"
  ([]
   (use-dimensions (hooks/use-ref nil)))
  ([node]
   (use-dimensions node :box))
  ([node sizing]
   (let [observer (hooks/use-ref nil)
         [dimensions set-dimensions!] (hooks/use-state nil)
         resize-idle-service (hooks/use-ref
                              (make-idle-service
                               20
                               (case sizing
                                   ;;
                                 :content
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
                                     (set-dimensions! dimensions)))
                                   ;; default
                                 (fn handle [entries]
                                   (let [[_ entry] (reverse entries)
                                         [box-size] (.-borderBoxSize entry)
                                         dimensions {:width (.-inlineSize box-size)
                                                     :height (.-blockSize box-size)}]
                                     (set-dimensions! dimensions))))))]
     (hooks/use-effect
       :always
       ;; If node isn't present
       (when (nil? @node)
         (set-dimensions! nil)
         (when @observer (.disconnect @observer)))
       ;; When node shows up and dimensions are nil
       ;; then add resize observer
       (when (and (some? @node) (nil? dimensions))
         (letfn [(reset [[entry]]
                   (async/put! @resize-idle-service entry))]
           (reset! observer (js/ResizeObserver. reset))
           (.observe @observer @node)
           (set-dimensions! (util/bounding-client-rect @node))
           nil)))
     (hooks/use-effect
       :once
       (fn []
         (when @observer (.disconnect @observer))))
     [node dimensions])))

(defhook use-scroll-offset
  "This hook is intended for infinite scroll. Threshold is
  how many pixels from bottom do you wan't to change state.

  Returns: `[offset reset]`
  
  It will have internal cache that will track users maximal
  scroll and if in threshold area it will simply inc offset,
  thus enabling you to use-effect and track that offset to
  handle what should happen. 
  
  Reset is function that is called without arguments to reset
  offset counter."
  ([body] (use-scroll-offset body 50))
  ([body threshold]
   (let [[offset set-offset!] (hooks/use-state 0)
         cached-height (hooks/use-ref 0)]
     (letfn [(check-offset [_]
               (let [scrolled (+ (.-scrollTop @body) (.-clientHeight @body))
                     content-height (.-scrollHeight @body)]
                 (when (and
                        (not= @cached-height content-height)
                        (>= (+ scrolled threshold) content-height))
                   (set-offset! inc)
                   (reset! cached-height content-height))))
             (reset []
               (reset! cached-height 0)
               (set-offset! 0))]
       (hooks/use-effect
         [@body]
         (when @body (.addEventListener @body "scroll" check-offset))
         (fn []
           (when @body (.removeEventListener @body "scroll" check-offset))))
       [offset reset]))))

(defhook use-multi-dimensions
  "Similar to use dimensions, only for tracking multiple elements.
  Input should be sequence of keys, i.e keywords, and output is vector
  of two elements. First element is map of key to React ref, and
  second is map of reference to element dimensions."
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

(defhook use-parent
  "Hook will return parent of _ref"
  [_ref]
  (util/dom-parent _ref))

(defhook ^{:deprecated true} use-on-parent-resized
  "Hook will track parent of _ref and when it is
  resized it will call handler"
  [_ref handler]
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
  
  `publisher` should be used with [[use-listener]]"
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

(defn random-string
  ([]
   (subs (.toString (.random js/Math) 36) 2)))

(defnc portal
  "Use when you wan't to mount react component on some
  DOM element that can be found by locator function.
  
  portal will try to locate element. If it is not found inside
  timeout period, portal will give up"
  [{:keys [timeout locator] :or {timeout 15000} :as props}]
  (let [[target set-target!] (hooks/use-state nil)
        _state (hooks/use-ref nil)
        now (.now js/Date)]
    (letfn [(safe-update
              [^js target]
              (let [state (.getAttribute target "portal-state")
                    new-state (random-string)]
                (when (or (nil? @_state) (not= state @_state))
                  (reset! _state new-state)
                  (.setAttribute target "portal-state" new-state)
                  (set-target! target))))]
      (when-some [target (locator)]
        (safe-update target))
      (use-toddler-listener
       :react/dangerously-rendered
       (fn []
         (when-some [target (locator)]
           (safe-update target))))
      (hooks/use-effect
        [target]
        (async/go-loop
         []
          (if-some [target (locator)]
            (safe-update target)
            (when (< (- (.now js/Date) now) timeout)
              (do
                (async/<! (async/timeout 40))
                (recur)))))))
    (when target
      (rdom/createPortal (children props) target))))

(defhook use-query
  "Hook will return function that when called
  will send GraphQL query to backend and return response
  in form of async channel.
  
  selection should be in lacinia compatible selection form.
  I.E.

  ```clojure
  {:name nil
   :address nil
   :relatives [{:selections {:name nil :address nil}
                :args {:city \"New York\"}}]}
  ```"
  ([{query-name :query
     selection :selection
     alias :alias
     args :args
     :keys [on-load on-error]}]
   (let [url (use-graphql-url)
         [token] (use-token)]
     (hooks/use-memo
       [(name query-name) args selection token]
       (fn send
         ([]
          (let [query-name (name query-name)
                query-key (keyword query-name)
                query (graphql/wrap-queries
                       (graphql/->graphql
                        (graphql/->GraphQLQuery
                         query-name alias selection args)))]
            (async/go
              (let [{:keys [errors]
                     {data query-key} :data
                     :as response}
                    (async/<!
                     (send-query
                      query
                      :url url
                      :token token
                      :on-load on-load
                      :on-error on-error))]
                (if (some? errors)
                  (ex-info "Remote query failed"
                           {:query query
                            :args args
                            :selection selection
                            :response response})
                  data))))))))))

(defhook use-queries
  "Hook will return function that when called
  will send queries to backend and return response
  in form of async channel.
  
  For more info about how to write queries look at
  [[use-query]]"
  ([queries] (use-queries queries nil))
  ([queries {:keys [on-load on-error]}]
   (let [url (use-graphql-url)
         [token] (use-token)]
     (hooks/use-memo
       [queries token]
       (fn send
         ([]
          (let [query (graphql/queries queries)]
            (async/go
              (let [{:keys [errors]
                     data :data
                     :as response}
                    (async/<!
                     (send-query
                      query
                      :url url
                      :token token
                      :on-load on-load
                      :on-error on-error))]
                (if (some? errors)
                  (ex-info "Remote query failed"
                           {:queries queries
                            :response response})
                  data))))))))))

(defhook use-mutation
  "Hook will return function that will send GraphQL
  mutation to backend based on input params.
  
  Returns async/chan"
  ([{:keys [mutation selection types alias args on-load on-error]}]
   (let [url (use-graphql-url)
         [token] (use-token)]
      ; (when (ifn? on-load) (on-load "109"))
     (hooks/use-callback
       [mutation selection types token args]
       (fn mutate
         ([data]
          (async/go
            (let [{:keys [query variables]}
                  (graphql/mutations [{:mutation mutation
                                       :selection selection
                                       :alias alias
                                       :args args
                                       :variables data
                                       :types types}])
                  ;;
                  {:keys [errors]
                   {data mutation} :data}
                  (async/<!
                   (send-query
                    query
                    :url url
                    :token token
                    :variables variables
                    :on-load on-load
                    :on-error on-error))]
              (if (some? errors)
                (ex-info
                 (str "Mutation " mutation " failed")
                 {:query query
                  :variables variables
                  :errors errors})
                data)))))))))

(defhook use-mutations
  "Wraps multiple mutation into single GraphQL
  query and returns function that will send mutation
  to backend based on input parameters"
  ([{:keys [mutations on-load on-error]}]
   (let [url (use-graphql-url)
         [token] (use-token)]
     (hooks/use-callback
       [mutations token]
       (fn []
         (async/go
           (let [{:keys [query variables]} (graphql/mutations mutations)
                 ;;
                 {:keys [errors]
                  data :data}
                 (async/<!
                  (send-query
                   query
                   :url url
                   :token token
                   :variables variables
                   :on-load on-load
                   :on-error on-error))]
             (if (some? errors)
               (ex-info
                (str "Mutations " (str/join ", " (map :mutation mutations)) " failed")
                {:query query
                 :variables variables
                 :errors errors})
               data))))))))
