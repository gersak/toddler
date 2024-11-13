;; Copyright (C) Robert Gersak - All Rights Reserved
;; Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <gersakr@gmail.com, June 2019

(ns toddler.router
  (:require
    [clojure.set :as set]
    goog.object
    [clojure.edn :as edn]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [cljs.reader :refer [read-string]]
    [helix.core
     :refer [defhook
             defnc
             create-context
             provider]]
    [helix.hooks :as hooks]
    [helix.children :refer [children]]
    [clojure.zip :as zip]
    [toddler.hooks :refer [use-translate use-delayed]]))



(def -dispatch- (create-context))
(def -router- (create-context))
(def -navigation- (create-context))
(def -history- (create-context))
(def -roles- (create-context))
(def -permissions- (create-context))
(def -super- (create-context))
(def -prefix- (create-context))


(defn location->map [^js location]
  {:pathname (.-pathname location)
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
  [{:keys [prefix] :as props}]
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
      {:context -prefix-
       :value prefix}
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


(defn maybe-add-prefix
  [prefix url]
  (if-not prefix url
    (if (str/ends-with? prefix "/") (apply str prefix (rest url))
      (str "/" prefix url))))


(defn maybe-remove-prefix
  [prefix url]
  (if-not prefix url
    (if (str/ends-with? prefix "/")
      (subs url (count prefix))
      (subs url (inc (count prefix))))))


(defhook use-query 
  "Hook returns [query-params query-setter]"
  ([] (use-query :replace))
  ([action]
    (let [{:keys [search] :as location} (use-location) 
          qp (js/URLSearchParams. search)
          prefix (hooks/use-context -prefix-)
          {push-url :push
           replace-url :replace} (use-navigate)
          setter (hooks/use-memo
                   [location]
                   (fn [params]
                     (let [query (clj->query params)
                           updated (maybe-add-prefix prefix (str (:pathname location) (when query (str \? query))))]
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
      (log/warn "Component %s already set in component tree")
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
      (str/join "/" (conj (mapv :segment parents) (:segment (zip/node location)))))))


(defn on-path? [tree path id]
  (when (some? path)
    (when-some [cp (component-path tree id)]
      (str/starts-with? path cp))))



(def last-rendered-key "toddler.router/last-rendered")


(defhook use-rendered? [id]
  (let [{original-pathname :pathname} (use-location)
        {:keys [tree]} (hooks/use-context -router-)
        prefix (hooks/use-context -prefix-)
        pathname (maybe-remove-prefix prefix original-pathname)
        on-path? (on-path? tree pathname id)]
    (when on-path?
      (.setItem js/sessionStorage last-rendered-key [id original-pathname]))
    on-path?))



(defhook use-component-name
  [id]
  (let [{:keys [tree]} (hooks/use-context -router-)
        translate (use-translate)
        {component-name :name}
        (when-some [location (component->location tree id)]
          (zip/node location))]
    (cond
      (string? component-name) component-name
      (keyword? component-name) (translate component-name)
      :else "")))


(defmethod reducer ::add-components
  [{:keys [tree unknown] :as state} {:keys [components parent]}]
  (let [state'
        (reduce
          (fn [{:keys [tree] :as state} component]
            (let [component (assoc component :parent parent)]
              (try
                ; (log/debugf "Trying to add component %s to parent %s " component parent)
                (let [tree' (set-component tree component)]
                  (log/debugf "Adding component %s to parent %s" component parent)
                  (assoc state :tree tree'))
                (catch js/Error _
                  (update state :unknown (fnil conj #{} component))))))
          {:tree tree
           :unknown unknown}
          components)
        state'' (reduce
                  (fn [{:keys [tree] :as state} component]
                    (try
                      (let [tree' (set-component tree component)]
                        (log/debugf "Adding component %s to parent %s" component parent)
                        (->
                          state 
                          (assoc :tree tree')
                          (update :unknown disj component)))
                      (catch js/Error _
                        (update state :unknown (fnil conj [] component)))))
                  state'
                  (:unknown state'))]
    ; (log/debugf "New Tree:\n%s" (with-out-str (pprint tree')))
    (merge state state'')))


(defhook use-component-children
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


(defhook use-authorized? [id]
  (let [user-permissions (hooks/use-context -permissions-)
        user-roles (hooks/use-context -roles-)
        super-role (hooks/use-context -super-)
        {:keys [tree]} (hooks/use-context -router-)
        {:keys [roles permissions] :as component}
        (when-some [location (component->location tree id)]
          (zip/node location))]
    (cond
      ;;
      (and (empty? user-roles) (empty? user-permissions))
      (do
        (log/warnf "[%s] Trying to use-authorized? when neither -permissions- or -roles context is not set. Check out your Protected component." id)
        false)
      ;;
      (and (some? user-permissions) (not (set? user-permissions)))
      (do
        (log/errorf "Trying to use-authorized? with -permissions- context set to %s. Instead it should be clojure set. Check out your Protected component." user-permissions)
        false)
      ;;
      (and (some? user-roles) (not (set? user-roles)))
      (do
        (log/errorf "Trying to use-authorized? with -roles- context set to %s. Instead it should be clojure set. Check out your Protected component." user-roles)
        false)
      ;;
      (nil? component)
      (do
        (log/debugf "[%s] Couldn't find component!" id)
        false)
      ;;
      (and (empty? roles) (empty? permissions))
      (do
        (log/warnf "[%s] Component has no role or permission protection" id)
        true)
      ;;
      (contains? user-roles super-role) true
      ;;
      :else
      (do
        (log/debugf "[%s] Checking component access for: %s" id user-roles)
        (or
          (not-empty (set/intersection roles user-roles))
          (not-empty (set/intersection permissions user-permissions)))))))


(defhook use-url->components
  []
  (let [{{url :pathname} :location
         :keys [tree]} (hooks/use-context -router-)
        prefix (hooks/use-context -prefix-)
        url (maybe-remove-prefix prefix url)]
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
        prefix (hooks/use-context -prefix-)
        path (component-path tree component)]
    (maybe-add-prefix prefix path)))


(defhook use-go-to
  [component]
  (let [{:keys [go]} (hooks/use-context -navigation-)
        {:keys [tree]} (hooks/use-context -router-)
        prefix (hooks/use-context -prefix-)]
    (if-some [url (component-path tree component)]
      (fn redirect
        ([] (redirect nil))
        ([params]
         (let [url (str (maybe-add-prefix prefix url) (when params (str "?" (clj->query params))))]
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
        prefix (hooks/use-context -prefix-)]
    (loop [position (component-tree-zipper tree)
           result []]
      (if (zip/end? position)
        (let [sorted (sort-by :landing result)
              {best :id} (last sorted)]
          (maybe-add-prefix prefix (component-path tree best))) 
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
        prefix (hooks/use-context -prefix-)]
    (letfn [(authorized?
              [{:keys [id roles permissions] :as component}]
              (cond
                ;;
                (and (empty? user-roles) (empty? user-permissions))
                (do
                  (log/warnf "[%s] Trying to use-authorized? when neither -permissions- or -roles context is not set. Check out your Protected component." id)
                  false)
                ;;
                (and (some? user-permissions) (not (set? user-permissions)))
                (do
                  (log/errorf "Trying to use-authorized? with -permissions- context set to %s. Instead it should be clojure set. Check out your Protected component." user-permissions)
                  false)
                ;;
                (and (some? user-roles) (not (set? user-roles)))
                (do
                  (log/errorf "Trying to use-authorized? with -roles- context set to %s. Instead it should be clojure set. Check out your Protected component." user-roles)
                  false)
                ;;
                (nil? component)
                (do
                  (log/debugf "[%s] Couldn't find component!" id)
                  false)
                ;;
                (and (empty? roles) (empty? permissions))
                (do
                  (log/warnf "[%s] Component has no role or permission protection" id)
                  true)
                ;;
                (contains? user-roles super-role) true
                ;;
                :else
                (do
                  (log/debugf "[%s] Checking component access for: %s" id user-roles)
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
            [last-component last-url] (edn/read-string (.getItem js/sessionStorage last-rendered-key))
            authorized? (use-authorized? last-component)
            [best] (get-landing-candidates)]
        (hooks/use-effect
          [tree (:id best)]
          (when (= location url)
            (cond
              authorized? (push last-url)
              (some? best) (push (maybe-add-prefix prefix (component-path tree (:id best))))
              :else nil)))))
    (children props)))




(defnc Protect
  [{:keys [super permissions roles] :as props}]
  (provider
    {:context -permissions-
     :value permissions}
    (provider
      {:context -roles-
       :value roles}
      (provider
        {:context -super-
         :value super}
        (children props)))))
