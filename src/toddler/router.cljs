(ns toddler.router
  "Routing in toddler is focused on
  linking components in component tree. This component
  tree is built by [[use-link]] hook or even better
  with [[wrap-link]] function. 
  
  I.E.

  ```clojure
  (defnc HelloWorld
   {:wrap [(wrap-link
            :toddler.router/ROOT
            [{:id ::component-1
              :name \"testing1\"}
             {:id ::component-2
              :name \"testing2\"}])]}
   []
   ($ World {:message \"Hey there!\"}))
  ```
  Other hooks and functions in this namespace are
  here to help you:
  
   * Navigate component tree by sending user to component URL
  or adding parameters in browser URL that will have effect
  on your application state
   * Check if component is rendered by comparing path in routing
  tree to current browser location.
   * Check if user has rights to access some route by
  protecting routes with :roles and :permissions

  Ensure that user is redirected to landing page that
  is ment for him by using [[LandingPage]] component
  "
  (:require
   [clojure.set :as set]
   [goog.string :refer [format]]
   goog.object
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   ; [taoensso.telemere :as t]
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

(def ^:no-doc -dispatch- (create-context))
(def ^:no-doc -landing- (create-context))
(def ^:no-doc -router- (create-context))
(def ^:no-doc -navigation- (create-context))
(def ^:no-doc -history- (create-context))
(def ^:no-doc -roles- (create-context))
(def ^:no-doc -permissions- (create-context))
(def ^:no-doc -super- (create-context))
(def ^:no-doc -base- (create-context))

(defn location->map
  "For given js/Location object will return
  hashmap that contains:
    :pathname
    :hash
    :origin
    :search"
  [^js location]
  {:pathname (.-pathname location)
   :hash (subs (.-hash location) 1)
   :origin (.-origin location)
   :search (.-search location)})

(defmulti reducer
  "Multifunction where you can extend router functionality.
  
  Default implementations exist for routing events of type:
  
   * :location/change
   * ::add-components"
  (fn [_ {:keys [type]}] type))

(defmethod reducer :location/change
  [state {:keys [value]}]
  (assoc state :location (location->map value)))

(defhook use-component-tree
  "Hook will return routing tree for -router- context"
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
  that as base URL
  
  ```
  ($ Provider
    {:base \"my-app\"}
    (d/div \"Hello world\"))"
  [{:keys [base] :as props}]
  (let [[router dispatch] (hooks/use-reducer
                           reducer
                           (merge
                            {:location (location->map js/window.location)
                             :tree {:id ::ROOT
                                    :segment ""
                                    :name nil
                                    :children []}}
                            (select-keys props [:id])))
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
    ; (cljs.pprint/pprint router)
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

(defn wrap-router
  "Wrapper that will use Provider component to
  encapsulate component"
  ([component]
   (fnc Router [props]
     ($ Provider ($ component {& props}))))
  ([component base]
   (fnc Authorized [props]
     (.log js/console "Wrapping Router with base '" base "'")
     ($ Provider {:base base} ($ component {& props})))))

(defhook use-location
  "Hook will return location from -router- context. Location
  will contain following keys:
  
  ```clojure
  {:pathname (.-pathname location)
   :hash (subs (.-hash location) 1)
   :origin (.-origin location)
   :search (.-search location)}
  ```"
  []
  (let [{:keys [location]} (hooks/use-context -router-)]
    location))

(defhook use-navigate
  "Hook will return value in -navigation- context"
  []
  (hooks/use-context -navigation-))

(defn clj->query
  "Function will turn clojure map into URLSearchParams"
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
  "Function will URLSearchParams into clojure map"
  [qp]
  (zipmap
   (map keyword (.keys qp))
   (map read-string (.values qp))))

(defn maybe-add-base
  "For given base and url will add base prefix
  if it exists to url. If base is nil than URL
  is returned"
  [base url]
  (if (empty? base)
    url
    (if (str/ends-with? base "/") (apply str base (rest url))
        (str "/" base url))))

(defn maybe-remove-base
  "For given base and url will return URL without
  base. If base is nil function will return URL
  immediately"
  [base url]
  (if (empty? base)
    url
    (as-> url url
      (if (str/ends-with? base "/")
        (subs url (count base))
        (subs url (inc (count base))))
      (if-not (str/starts-with? base "/") url
              (str "/" url)))))

(defhook use-query
  "Hook returns `[query-params query-setter]`. Query params are
  values that are pulled from URLSearchParams and query-setter
  is function that when called will set URLSearchParams"
  ([] (use-query :replace))
  ([action]
   (let [{:keys [search] :as location} (use-location)
         qp (js/URLSearchParams. search)
         {push-url :push
          replace-url :replace} (use-navigate)
         setter (hooks/use-memo
                  [location]
                  (fn [params]
                    (let [updated (if (nil? params)
                                    (str/replace (:pathname location) #"\?.*" "")
                                    (str (:pathname location) (let [query (clj->query params)]
                                                                (str \? query))))]
                      (case action
                        :replace (replace-url updated)
                        :push (push-url updated)))))]
     [(query->clj qp) setter])))

(defn component-tree-zipper
  "Function returns routing tree zipper"
  [root]
  (zip/zipper
   :children
   :children
   (fn [node children] (assoc node :children (vec children)))
   root))

(defonce ^{:dynamic true
           :doc "Component tree cache. This is where toddler router adds
                components and looks for routing information."}
  *component-tree*
  (atom
   {:id ::ROOT
    :segment ""
    :name nil
    :children []}))

(defn component->location
  "For given component tree and component id
  function will return zipper location if id is
  found."
  [tree id]
  (let [z (component-tree-zipper tree)]
    (loop [p z]
      (if (zip/end? p) nil
          (let [{id' :id} (zip/node p)]
            (if (#{id} id')
              p
              (recur (zip/next p))))))))

(defn set-component
  "Function used to add component to component tree.
  For given component tree add component by specifying
  component id and component parent."
  [tree {:keys [id parent children] :as component}]
  (if (component->location tree id)
    (do
      ; (t/log! :warn (format "Component %s already set in component tree" id))
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
  "Function will remove component with id from component tree"
  [tree id]
  (if-let [location (component->location tree id)]
    (-> location
        zip/remove
        zip/root)
    tree))

(let [cache (atom nil)]
  (defn component-path
    "Function will walk component tree to find component
    with id and when found will return all URL for that
    component."
    [tree id]
    (if-let [path (get @cache id)]
      path
      (let [location (component->location tree id)
            parents (when location (zip/path location))]
        (when location
          (let [{:keys [segment hash]} (zip/node location)
                path (str/join "/" (cond-> (mapv :segment parents)
                                     (not-empty segment) (conj segment)
                                     (not-empty hash) (conj (str "#" hash))))]
            (swap! cache assoc :id path)
            path))))))

(defn on-path?
  "For given path and component id function will get
  component path and check if given path starts with
  component path.
  
  If it does, than component is on path (true)"
  [tree path id]
  (when (some? path)
    (when-some [cp (component-path tree id)]
      (str/starts-with? path (first (str/split cp #"\#"))))))

(def ^:no-doc last-rendered-key "toddler.router/last-rendered")

(defhook use-rendered?
  "Hook will return true if component with id
  is rendered, by checking if browser location
  contains component path."
  [id]
  (let [{original-pathname :pathname} (use-location)
        {:keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        on-path? (hooks/use-memo
                   [tree base original-pathname]
                   (on-path? tree (maybe-remove-base base original-pathname) id))]
    on-path?))

(defhook use-component-name
  "For given component id, hook will return
  component name.
  
  If component :name was keyword it will try to translate
  that keyword.
  
  If component :name is string it won't translate. Just return that name
  
  If component doesn't have :name, hook will try to translate
  component id (only if component exists in component tree)"
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
  [{:keys [known]
    :or {known #{}}
    :as state} {:keys [components parent]}]
  (if-let [to-register (not-empty (remove (comp known :id) components))]
    (as-> state _state
      (reduce
       (fn [{:keys [tree] :as state} {:keys [id children] :as component}]
         (let [component (assoc component :parent parent)]
           (try
              ; (log/debugf "Trying to add component %s to parent %s " component parent)
             (let [tree' (set-component tree component)
                   vanilla (->
                            state
                            (assoc :tree tree')
                            (update :known (fnil conj #{}) (:id component)))]
               (if (empty? children)
                 vanilla
                 (reducer
                  vanilla
                  {:type ::add-components
                   :parent id
                   :components children})))
             (catch js/Error _
               (update state :unknown
                       (fn [components]
                         (vec
                          (distinct
                           ((fnil conj []) components component)))))))))
       _state
       to-register)
      (reduce
       (fn [{:keys [tree] :as state} {:keys [id children] :as component}]
         (try
           (let [tree' (set-component tree component)
                 vanilla (->
                          state
                          (assoc :tree tree')
                          (update :unknown (fn [components] (vec (remove #{component} components))))
                          (update :known (fnil conj #{}) (:id component)))]
             (if (empty? children)
               vanilla
               (reducer
                vanilla
                {:type ::add-components
                 :parent id
                 :components children})))
           (catch js/Error _
             (update state :unknown
                     (fn [components]
                       (vec
                        (distinct
                         ((fnil conj []) components component))))))))
       _state
       (:unknown state))
      (merge state _state))
    state))

(defhook use-link
  "Hook will link parent with children components and add that to
  component tree for current -router- context. Children is expected
  to be map of:

   * :id - Component ID. Should uniquely identify component
   * :name - Name of component. Can be used to resolve what to display.
             If :name is of type string than use-component-name hook
             will return that name.  
             When keyword is used as name value use-component-name will
             try to resolve that keyword as translation in respect to
             locale in current app/locale context.
   * :hash - Optional hash that is appended to component URL
   * :segment - Segment of path that is conjoined to all parent segments. Used
                to resolve if component is rendered or not and in use-go-to
                hook to resolve what is target path if I wan't to \"go\" to
                component with id
   * :roles - #{} with roles that are allowed to access this component
   * :permissions - #{} with permissions that are allowed to access this component
   * :landing - `[number]` to mark this component as possible landing site with number priority
  
  Linking should start with parent :toddler.router/ROOT component, as this
  component is parent to all other components"
  [parent children]
  (let [dispatch (hooks/use-context -dispatch-)
        children (if (map? children) [children] children)]
    (when (nil? dispatch)
      (.error js/console "Router provider not initialized. Use Provider from this namespace and instantiate it in one of parent components!"))
    (hooks/use-effect
      :once
      (dispatch
       {:type ::add-components
        :components children
        :parent parent}))))

(defhook use-is-super?
  "Hook that will return true if user is in super roles
  or false if he isn't"
  []
  (let [super-role (hooks/use-context -super-)
        user-roles (hooks/use-context -roles-)]
    (contains? user-roles super-role)))

(defhook use-authorized?
  "Hook that will return true if user is authorized
  to access component with \"id\". If not will return
  false.
  
  If ID is ommited, than authorized will check if user
  is superuser."
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
       (or
        super?
        (cond
         ;;
          (and (empty? user-roles) (empty? user-permissions))
          (do
            ; (t/log! :warn (format "[%s] Trying to use-authorized? when neither -permissions- or -roles context is not set. Check out your Protected component." id))
            false)
         ;;
          (and (some? user-permissions) (not (set? user-permissions)))
          (do
            ; (t/log! :error (format "Trying to use-authorized? with -permissions- context set to %s. Instead it should be clojure set. Check out your Protected component." user-permissions))
            false)
         ;;
          (and (some? user-roles) (not (set? user-roles)))
          (do
            ; (t/log! :error (format "Trying to use-authorized? with -roles- context set to %s. Instead it should be clojure set. Check out your Protected component." user-roles))
            false)
         ;;
          (nil? component)
          (do
            ; (t/log! :debug (format "[%s] Couldn't find component!" id))
            false)
         ;;
          (and (empty? roles) (empty? permissions))
          (do
            ; (t/log! :warn (format "[%s] Component has no role or permission protection" id))
            true)
         ;;
          :else
          (do
            ; (t/log! :debug (format "[%s] Checking component access for: %s" id user-roles))
            (or
             (not-empty (set/intersection roles user-roles))
             (not-empty (set/intersection permissions user-permissions))))))))))

(defnc Authorized
  "Wrapper component that will render children if user is authorized to
  access component with :id in props"
  [{:keys [id] :as props}]
  (let [authorized? (use-authorized? id)]
    (when authorized?
      (children props))))

(defn wrap-authorized
  "Wrapper that will use Authorized component to
  render children if user is authorized"
  ([component]
   (fnc Authorized [props]
     ($ Authorized ($ component {& props}))))
  ([component id]
   (fnc Authorized [props]
     ($ Authorized {:id id} ($ component {& props})))))

(defnc Rendered
  "Component will render children if compnoent with :id
  from props is active (is contained in current URL)"
  [{:keys [id] :as props}]
  (let [rendered? (use-rendered? id)]
    (when rendered?
      (children props))))

(defn wrap-rendered
  "Wrapper that will use Rendered component to
  render children if navigation is at component
  with id"
  ([component]
   (fnc Rendered [props]
     ($ Rendered ($ component {& props}))))
  ([component id]
   (fnc Rendered [props]
     ($ Rendered {:id id} ($ component {& props})))))

(defnc Link
  "Component will link parrent with id and children
  with routing info. Check out [[use-link]] hook
  to see how to structure children routing info."
  [{:keys [parent links] :as props}]
  (use-link parent links)
  (children props))

(defn wrap-link
  "Utility function to link component with children.
  Will use [[Link]]."
  ([component parent children]
   (fnc Linked [props]
     ($ Link {:parent parent :links children}
        ($ component {& props})))))

(defn- url->components
  [tree url]
  (loop [position (component-tree-zipper tree)
         result []]
    (if (zip/end? position) result
        (let [node (zip/node position)]
          (cond
            (nil? node) (recur (zip/next position) result)
            (on-path? tree url (:id node)) (recur (zip/next position) (conj result (dissoc node :children)))
            :else (recur (zip/next position) result))))))

(defhook use-url->components
  "Hook will return sequence of rendered components for
  current browser location"
  []
  (let [{{url :pathname} :location
         :keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        url (maybe-remove-base base url)]
    (when tree
      (url->components tree url))))

(defhook use-component-path
  "Hook will return url for `component[id]`"
  [component]
  (let [{:keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        path (component-path tree component)]
    (maybe-add-base base path)))

(defhook use-go-to
  "Hook will return function that will redirect browser
  to `component[id]`. Returned function can be called with
  parameters, and those parameters will be set in URL
  query."
  [component]
  (let [{:keys [go]} (hooks/use-context -navigation-)
        {:keys [tree]} (hooks/use-context -router-)
        base (hooks/use-context -base-)
        base (cond
               (nil? base) base
               ;;
               (not (string? base)) (.warn js/console "Base should be string: " (pr-str base))
               ;;
               (str/starts-with? base "/")
               (do
                 (.warn js/console "Provided base to toddler.router/Provider shouldn't start with '/'")
                 (subs base 1))
               :else base)
        url (hooks/use-memo
              [tree go]
              (component-path tree component))]
    (if url
      (fn redirect
        ([] (redirect nil))
        ([params]
         (let [url (str (maybe-add-base base url) (when params (str "?" (clj->query params))))]
           (go url))))
      (fn [& _]
        (.error js/console "Couldn't find component: " component)
        (pprint tree)))))

(defnc LandingPage
  "Component that is active when routing location is
  at :url from props. If so, then this component will
  look for :landing `priority` in `*component-tree*` and sort
  all found components by priority.
  
  Component with highest priority is chosen and its URL
  is computed and user agent is redirected to that URL."
  [{:keys [url enforce-access?]
    :or {enforce-access? true}
    :as props}]
  (let [user-permissions (hooks/use-context -permissions-)
        user-roles (hooks/use-context -roles-)
        super-role (hooks/use-context -super-)
        {:keys [tree]} (hooks/use-context -router-)
        tree (use-delayed tree)
        base (hooks/use-context -base-)]
    (letfn [(authorized?
              [{:keys [id roles permissions] :as component}]
              (if-not enforce-access?
                true
                (cond
                  ;;
                  (and (empty? user-roles) (empty? user-permissions))
                  (do
                    ; (t/log! :warn (format "[%s] Trying to use-authorized? when neither -permissions- or -roles context is not set. Check out your Protected component." id))
                    false)
                  ;;
                  (and (some? user-permissions) (not (set? user-permissions)))
                  (do
                    ; (t/log! :error (format "Trying to use-authorized? with -permissions- context set to %s. Instead it should be clojure set. Check out your Protected component." user-permissions))
                    false)
                  ;;
                  (and (some? user-roles) (not (set? user-roles)))
                  (do
                    ; (t/log! :error (format "Trying to use-authorized? with -roles- context set to %s. Instead it should be clojure set. Check out your Protected component." user-roles))
                    false)
                  ;;
                  (nil? component)
                  (do
                    ; (t/log! :debug (format "[%s] Couldn't find component!" id))
                    false)
                  ;;
                  (and (empty? roles) (empty? permissions))
                  (do
                    ; (t/log! :warn (format "[%s] Component has no role or permission protection" id))
                    true)
                  ;;
                  (contains? user-roles super-role) true
                  ;;
                  :else
                  (do
                    ; (t/log! :debug (format "[%s] Checking component access for: %s" id user-roles))
                    (or
                     (not-empty (set/intersection roles user-roles))
                     (not-empty (set/intersection permissions user-permissions)))))))
            (get-landing-candidates
              [tree]
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
                     (get-landing-candidates tree))]
        (hooks/use-effect
          [tree (:id best) location]
          (let [relative-location (maybe-remove-base base location)]
            (cond
              (and (= (maybe-remove-base base location) url)
                   (some? best))
              (push (maybe-add-base base (component-path tree (:id best))))
              ;;
              (and (not-empty (:children tree))
                   (not= relative-location url))
              (let [rendered-components (url->components tree relative-location)]
                (when (>= 1 (count rendered-components))
                  (.error js/console (str "Zero rendered components found. Redirecting to " (pr-str best)))
                  (push (maybe-add-base base (component-path tree (:id best))))))
              :else nil)))))
    (provider
     {:context -landing-
      :value url}
     (children props))))

(defn wrap-landing
  "Wrapper that will use LandingPage component to
  encapsulate component"
  ([component url]
   (wrap-landing component url true))
  ([component url enforce-access?]
   (fnc Authorized [props]
     ($ LandingPage {:url url :enforce-access? enforce-access?}
        ($ component {& props})))))

(defhook use-landing
  []
  (hooks/use-context -landing-))

(defnc Protect
  "Component will set protection contexts. Contexts like
  -permissions-, -roles- and -super-.
  
  Use it as high as possible in your rendered app."
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
