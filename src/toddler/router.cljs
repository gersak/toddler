(ns toddler.router
  (:require
   [clojure.set :as set]
   [clojure.core.async :as async]
   [goog.string :refer [format]]
   goog.object
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [taoensso.telemere :as t]
   [cljs.reader :refer [read-string]]
   [helix.core
    :refer [defhook
            defnc
            fnc
            create-context
            provider
            $]]
   [helix.hooks :as hooks]
   [helix.children :refer [children]]
   [clojure.zip :as zip]
   [toddler.core :refer [use-translate use-delayed]]))

(def -dispatch- (create-context))
(def -router- (create-context))
(def -navigation- (create-context))
(def -history- (create-context))
(def -roles- (create-context))
(def -permissions- (create-context))
(def -super- (create-context))
(def -base- (create-context))

(defn location->map [^js location]
  {:pathname (.-pathname location)
   :hash (subs (.-hash location) 1)
   :origin (.-origin location)
   :search (.-search location)})

(defmulti reducer (fn [_ {:keys [type]}] type))

(defmethod reducer :location/change
  [state {:keys [value]}]
  (assoc state :location (location->map value)))

(defhook use-component-tree
  []
  (let [{:keys [tree]} (hooks/use-context -router-)]
    tree))

(defnc Provider
  "Component will wrap js/window history functionalities for pushing
  poping, replacing history as well as navigation functions like back,
  forward and go.
  
  This functions are provided in -navigation- context.
  
  Another context is provided, and that is -router- context that is
  holds information about current location and component tree.
  
  dispatch function to interact with -router- reducer is available
  through -dispatch- context.
  
  
  base - is base that this Provider should include in its context. I.E.
  if your application is served under /some/url than you should specify
  that as base URL"
  [{:keys [base] :as props}]
  (let [[router dispatch] (hooks/use-reducer
                           reducer
                           {:location (location->map js/window.location)
                            :tree {:id ::ROOT
                                   :segment ""
                                   :name nil
                                   :children []}})
        navigation (hooks/use-memo
                     :once
                     {:go (fn go
                            ([to] (go to false))
                            ([to replace]
                             (if replace
                               (.replaceState (.-history js/window) nil "" to)
                               (.pushState (.-history js/window) nil "" to))
                             (.dispatchEvent js/window (js/PopStateEvent. "popstate" {:state nil}))))
                      :replace (fn [to]
                                 (.replaceState (.-history js/window) nil "" to)
                                 (.dispatchEvent js/window (js/PopStateEvent. "popstate" {:state nil})))
                      :push (fn [to]
                              (.pushState (.-history js/window) nil "" to)
                              (.dispatchEvent js/window (js/PopStateEvent. "popstate" {:state nil})))
                      :back (fn []
                              (.back (.-history js/window))
                              (.dispatchEvent js/window (js/PopStateEvent. "popstate" {:state nil})))
                      :forward (fn []
                                 (.forward (.-history js/window))
                                 (.dispatchEvent js/window (js/PopStateEvent. "popstate" {:state nil})))})]
    (hooks/use-effect
      :once
      (letfn [(handle-change [_]
                (dispatch
                 {:type :location/change
                  :value js/window.location}))]
        (.addEventListener js/window "popstate" handle-change)
        (fn []
          (.removeEventListener js/window "popstate" handle-change))))
    (provider
     {:context -base-
      :value base}
     (provider
      {:context -router-
       :value router}
      (provider
       {:context -navigation-
        :value navigation}
       (provider
        {:context -dispatch-
         :value dispatch}
        (children props)))))))

(defhook use-location []
  (let [{:keys [location]} (hooks/use-context -router-)]
    location))

(defhook use-navigate
  []
  (hooks/use-context -navigation-))

(defn clj->query
  [data]
  (let [qp (js/URLSearchParams.)]
    (str
     (reduce-kv
      (fn [qp k v]
        (.append qp (name k) (pr-str v))
        qp)
      qp
      data))))

(defn query->clj
  [qp]
  (zipmap
   (map keyword (.keys qp))
   (map read-string (.values qp))))

(defn maybe-add-base
  [base url]
  (if-not base url
          (if (str/ends-with? base "/") (apply str base (rest url))
              (str "/" base url))))

(defn maybe-remove-base
  [base url]
  (if-not base url
          (if (str/ends-with? base "/")
            (subs url (count base))
            (subs url (inc (count base))))))

(defhook use-query
  "Hook returns [query-params query-setter]"
  ([] (use-query :replace))
  ([action]
   (let [{:keys [search] :as location} (use-location)
         qp (js/URLSearchParams. search)
         base (hooks/use-context -base-)
         {push-url :push
          replace-url :replace} (use-navigate)
         setter (hooks/use-memo
                  [location]
                  (fn [params]
                    (let [query (clj->query params)
                          updated (str (:pathname location) (when query (str \? query)))]
                      (case action
                        :replace (replace-url updated)
                        :push (push-url updated)))))]
     [(query->clj qp) setter])))

(defn component-tree-zipper [root]
  (zip/zipper
   :children
   :children
   (fn [node children] (assoc node :children (vec children)))
   root))

(defonce ^:dynamic *component-tree*
  (atom
   {:id ::ROOT
    :segment ""
    :name nil
    :children []}))

(defn component->location [tree id]
  (let [z (component-tree-zipper tree)]
    (loop [p z]
      (if (zip/end? p) nil
          (let [{id' :id} (zip/node p)]
            (if (#{id} id')
              p
              (recur (zip/next p))))))))

(defn set-component
  [tree {:keys [id parent] :as component}]
  (if (component->location tree id)
    (do
      (t/log! :warn (format "Component %s already set in component tree" id))
      tree)
    (if-let [location (component->location tree parent)]
      (->
       location
       (zip/append-child (->
                          component
                          (dissoc :parent)
                          (assoc :children [])))
       (zip/root))
      (throw
       (ex-info "Couldn't find parent"
                {:component component
                 :parent parent
                 :tree tree})))))

(defn remove-component
  [tree id]
  (if-let [location (component->location tree id)]
    (-> location
        zip/remove
        zip/root)
    tree))

(defn component-path
  [tree id]
  (let [location (component->location tree id)
        parents (when location (zip/path location))]
    (when location
      (let [{:keys [segment hash]} (zip/node location)]
        (str/join "/" (cond-> (mapv :segment parents)
                        (not-empty segment) (conj segment)
                        (not-empty hash) (conj (str "#" hash))))))))

(defn on-path? [tree path id]
  (when (some? path)
    (when-some [cp (component-path tree id)]
      (str/starts-with? path (first (str/split cp #"\#"))))))

(def last-rendered-key "toddler.router/last-rendered")

(defhook use-rendered? [id]
  (let [{original-pathname :pathname
         hash :hash} (use-location)
        {:keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        on-path? (hooks/use-memo
                   [tree base original-pathname]
                   (on-path? tree (maybe-remove-base base original-pathname) id))]
    (when on-path?
      (.setItem js/sessionStorage last-rendered-key [id original-pathname]))
    (hooks/use-effect
      [hash]
      (when hash
        (async/go
          (async/<! (async/timeout 1000))
          (when-some [el (.getElementById js/document hash)]
            (.scrollIntoView el #js {:block "start" :behavior "smooth"})))))
    on-path?))

(defhook use-component-name
  [id]
  (let [{:keys [tree]} (hooks/use-context -router-)
        translate (use-translate)
        {component-name :name
         :as location}
        (when-some [location (component->location tree id)]
          (zip/node location))]
    (cond
      (string? component-name) component-name
      (keyword? component-name) (translate component-name)
      (some? location) (translate id)
      :else "")))

(defmethod reducer ::add-components
  [{:keys [tree unknown known]
    :or {known #{}}
    :as state} {:keys [components parent]}]
  (if-let [to-register (not-empty (remove (comp known :id) components))]
    (let [state'
          (reduce
           (fn [{:keys [tree] :as state} component]
             (let [component (assoc component :parent parent)]
               (try
                  ; (log/debugf "Trying to add component %s to parent %s " component parent)
                 (let [tree' (set-component tree component)]
                   (t/log! :debug (format "Adding component %s to parent %s" component parent))
                   (->
                    state
                    (assoc :tree tree')
                    (update :known (fnil conj #{}) (:id component))))
                 (catch js/Error _
                   (update state :unknown (fnil conj #{} component))))))
           {:tree tree
            :unknown unknown}
           to-register)
          state'' (reduce
                   (fn [{:keys [tree] :as state} component]
                     (try
                       (let [tree' (set-component tree component)]
                         (t/log! :debug (format "Adding component %s to parent %s" component parent))
                         (->
                          state
                          (assoc :tree tree')
                          (update :unknown disj component)
                          (update :known (fnil conj #{}) (:id component))))
                       (catch js/Error _
                         (update state :unknown (fnil conj [] component)))))
                   state'
                   (:unknown state'))]
      ; (log/debugf "New Tree:\n%s" (with-out-str (pprint tree')))
      (merge state state''))
    state))

(defhook use-link
  "Hook will link parent with children components and add that to
  component tree for current -router- context. Children is expected
  to be map of:

  :id          Component ID. Should uniquely identify component

  :name        Name of component. Can be used to resolve what to display.
               If :name is of type string than use-component-name hook
               will return that name.
           
               When keyword is used as name value use-component-name will
               try to resolve that keyword as translation in respect to
               locale in current app/locale context.

  :hash        Optional hash that is appended to component URL

  :segment     Segment of path that is conjoined to all parent segments. Used
               to resolve if component is rendered or not and in use-go-to
               hook to resolve what is target path if I wan't to \"go\" to
               component with id
  
  :roles       #{} with roles that are allowed to access this component

  :permissions #{} with permissions that are allowed to access this component
  
  :landing     [number] to mark this component as possible landing site with number priority
  
  Linking should start with parent :toddler.router/ROOT component, as this
  component is parent to all other components"
  [parent children]
  (let [dispatch (hooks/use-context -dispatch-)]
    (when (nil? dispatch)
      (.error js/console "Router provider not initialized. Use Provider from this namespace and instantiate it in one of parent components!"))
    (hooks/use-layout-effect
      :once
      (dispatch
       {:type ::add-components
        :components children
        :parent parent}))))

;; DEPRECATED - use-link instead
(def use-component-children use-link)

(defhook use-is-super?
  []
  (let [super-role (hooks/use-context -super-)
        user-roles (hooks/use-context -roles-)]
    (contains? user-roles super-role)))

(defhook use-authorized?
  ([] (use-authorized? nil))
  ([id]
   (let [user-permissions (hooks/use-context -permissions-)
         user-roles (hooks/use-context -roles-)
         super-role (hooks/use-context -super-)
         {:keys [tree]} (hooks/use-context -router-)
         {:keys [roles permissions] :as component}
         (when-some [location (component->location tree id)]
           (zip/node location))
         super? (contains? user-roles super-role)]
     (hooks/use-memo
       [user-roles super-role]
       (cond
         ;;
         (nil? id) super?
         ;;
         (and (empty? user-roles) (empty? user-permissions))
         (do
           (t/log! :warn (format "[%s] Trying to use-authorized? when neither -permissions- or -roles context is not set. Check out your Protected component." id))
           false)
         ;;
         (and (some? user-permissions) (not (set? user-permissions)))
         (do
           (t/log! :error (format "Trying to use-authorized? with -permissions- context set to %s. Instead it should be clojure set. Check out your Protected component." user-permissions))
           false)
         ;;
         (and (some? user-roles) (not (set? user-roles)))
         (do
           (t/log! :error (format "Trying to use-authorized? with -roles- context set to %s. Instead it should be clojure set. Check out your Protected component." user-roles))
           false)
         ;;
         (nil? component)
         (do
           (t/log! :debug (format "[%s] Couldn't find component!" id))
           false)
         ;;
         (and (empty? roles) (empty? permissions))
         (do
           (t/log! :warn (format "[%s] Component has no role or permission protection" id))
           true)
         ;;
         super? true
         ;;
         :else
         (do
           (t/log! :debug (format "[%s] Checking component access for: %s" id user-roles))
           (or
            (not-empty (set/intersection roles user-roles))
            (not-empty (set/intersection permissions user-permissions)))))))))

(defnc Authorized
  [{:keys [id] :as props}]
  (let [authorized? (use-authorized? id)]
    (when authorized?
      (children props))))

(defn wrap-authorized
  ([component]
   (fnc Authorized [props]
     ($ Authorized ($ component {& props}))))
  ([component id]
   (fnc Authorized [props]
     ($ Authorized {:id id} ($ component {& props})))))

(defnc Rendered
  [{:keys [id] :as props}]
  (let [rendered? (use-rendered? id)]
    (when rendered?
      (children props))))

(defn wrap-rendered
  ([component]
   (fnc Rendered [props]
     ($ Rendered ($ component {& props}))))
  ([component id]
   (fnc Rendered [props]
     ($ Rendered {:id id} ($ component {& props})))))

(defhook use-url->components
  []
  (let [{{url :pathname} :location
         :keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        url (maybe-remove-base base url)]
    (when tree
      (loop [position (component-tree-zipper tree)
             result []]
        (if (zip/end? position) result
            (let [node (zip/node position)]
              (cond
                (nil? node) (recur (zip/next position) result)
                (on-path? tree url (:id node)) (recur (zip/next position) (conj result (dissoc node :children)))
                :else (recur (zip/next position) result))))))))

(defhook use-component-path
  [component]
  (let [{:keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        path (component-path tree component)]
    (maybe-add-base base path)))

(defhook use-go-to
  [component]
  (let [{:keys [go]} (hooks/use-context -navigation-)
        {:keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)]
    (if-some [url (component-path tree component)]
      (fn redirect
        ([] (redirect nil))
        ([params]
         (let [url (str (maybe-add-base base url) (when params (str "?" (clj->query params))))]
           (go url))))
      (fn [& _]
        (.error js/console "Couldn't find component: " component)
        (pprint tree)))))

(defhook use-last-rendered
  []
  (hooks/use-callback
    :once
    (fn []
      (let [[_ url] (edn/read-string (.getItem js/sessionStorage last-rendered-key))]
        url))))

(defhook use-landing
  []
  (let [{:keys [tree]} (hooks/use-context -router-)
        tree (use-delayed tree)
        base (hooks/use-context -base-)]
    (loop [position (component-tree-zipper tree)
           result []]
      (if (zip/end? position)
        (let [sorted (sort-by :landing result)
              {best :id} (last sorted)]
          (maybe-add-base base (component-path tree best)))
        (let [{:keys [landing] :as node} (zip/node position)]
          (cond
            (nil? node) (recur (zip/next position) result)
            landing (recur (zip/next position) (conj result (dissoc node :children)))
            :else (recur (zip/next position) result)))))))

(defnc LandingPage
  [{:keys [url] :as props}]
  (let [user-permissions (hooks/use-context -permissions-)
        user-roles (hooks/use-context -roles-)
        super-role (hooks/use-context -super-)
        {:keys [tree]} (hooks/use-context -router-)
        tree (use-delayed tree)
        base (hooks/use-context -base-)]
    (letfn [(authorized?
              [{:keys [id roles permissions] :as component}]
              (cond
                ;;
                (and (empty? user-roles) (empty? user-permissions))
                (do
                  (t/log! :warn (format "[%s] Trying to use-authorized? when neither -permissions- or -roles context is not set. Check out your Protected component." id))
                  false)
                ;;
                (and (some? user-permissions) (not (set? user-permissions)))
                (do
                  (t/log! :error (format "Trying to use-authorized? with -permissions- context set to %s. Instead it should be clojure set. Check out your Protected component." user-permissions))
                  false)
                ;;
                (and (some? user-roles) (not (set? user-roles)))
                (do
                  (t/log! :error (format "Trying to use-authorized? with -roles- context set to %s. Instead it should be clojure set. Check out your Protected component." user-roles))
                  false)
                ;;
                (nil? component)
                (do
                  (t/log! :debug (format "[%s] Couldn't find component!" id))
                  false)
                ;;
                (and (empty? roles) (empty? permissions))
                (do
                  (t/log! :warn (format "[%s] Component has no role or permission protection" id))
                  true)
                ;;
                (contains? user-roles super-role) true
                ;;
                :else
                (do
                  (t/log! :debug (format "[%s] Checking component access for: %s" id user-roles))
                  (or
                   (not-empty (set/intersection roles user-roles))
                   (not-empty (set/intersection permissions user-permissions))))))
            (get-landing-candidates
              []
              (loop [position (component-tree-zipper tree)
                     result []]
                (if (zip/end? position)
                  (reverse (sort-by :landing (filter authorized? result)))
                  (let [{:keys [landing] :as node} (zip/node position)]
                    (cond
                      (nil? node) (recur (zip/next position) result)
                      landing (recur (zip/next position) (conj result (dissoc node :children)))
                      :else (recur (zip/next position) result))))))]
      (let [{location :pathname} (use-location)
            {:keys [push]} (use-navigate)
            [best] (hooks/use-memo
                     [user-permissions user-roles super-role tree base]
                     (get-landing-candidates))]
        (hooks/use-effect
          [tree (:id best)]
          (when (= location url)
            (let [[last-component last-url] (edn/read-string (.getItem js/sessionStorage last-rendered-key))
                  component (when-some [location (component->location tree last-component)]
                              (zip/node location))
                  authorized? (authorized? component)]
              (cond
                authorized? (push last-url)
                (some? best) (push (maybe-add-base base (component-path tree (:id best))))
                :else nil))))))
    (children props)))

(defnc Protect
  [{:keys [super permissions roles] :as props}]
  (let [_super (hooks/use-context -super-)
        _permissions (hooks/use-context -permissions-)
        _roles (hooks/use-context -roles-)]
    (provider
     {:context -permissions-
      :value (or permissions _permissions)}
     (provider
      {:context -roles-
       :value (or roles _roles)}
      (provider
       {:context -super-
        :value (or super _super)}
       (children props))))))
