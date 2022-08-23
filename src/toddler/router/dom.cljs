(ns toddler.router.dom
  (:require
    clojure.set
    goog.object
    [cljs-bean.core :refer [->clj ->js]]
    [camel-snake-kebab.core :as csk]
    [cljs.reader :refer [read-string]]
    ["react-router-dom" :as router]
    [helix.core :refer [defhook]]
    [helix.hooks :as hooks]))



(defhook use-href [to] (router/useHref (->js to)))

(defhook use-navigate []
  (let [n (router/useNavigate)]
    (hooks/use-memo
      [n]
      (fn navigate
        ([to] (n to nil))
        ([to options] (n to (->js options)))))))


(defn- query->clj
  [qp]
  (zipmap
    (map keyword (.keys qp))
    (map read-string (.values qp))))


(defn- clj->query
  [data]
  (let [qp (js/URLSearchParams.)]
    (str 
      (reduce-kv
        (fn [qp k v]
          (.append qp (name k) (pr-str v))
          qp)
        qp
        data))))

(defhook use-search-params
  []
  (let [[search-params set-search-params!] (router/useSearchParams)
        set-params! (hooks/use-memo
                      [set-search-params!]
                      (fn [data]
                        (set-search-params! (clj->query data))))
        params (hooks/use-memo
                 [search-params]
                 (query->clj search-params))]
    [params set-params!]))


(def BrowserRouter router/BrowserRouter)
(def HashRouter router/HashRouter)
(def MemoryRouter router/MemoryRouter)
(def HistoryRouter router/HistoryRouter)
