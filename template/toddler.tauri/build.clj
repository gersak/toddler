(ns build
  (:require
   [clojure.edn :as edn]
   [clojure.tools.build.api :as b]
   [toddler.template :as template]))

(defonce salt (template/random-string))
(defonce index-file (format "/docs.index.%s.edn" salt))

(defn frontend-config
  []
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        {{main-build :dev} :builds} config]
    (->
     main-build
     (update :modules (fn [current]
                        (reduce-kv
                         (fn [r k v]
                           (assoc r (keyword (str (name k) "." salt))
                                  (if (contains? v :depends-on)
                                    (update v :depends-on
                                            (fn [deps]
                                              (reduce
                                               (fn [r d]
                                                 (conj r (keyword (str (name d) \. salt))))
                                               #{}
                                               deps)))
                                    v)))
                         nil
                         current)))
     (dissoc :output-to :output-dir)
     (assoc :closure-defines `{toddler.tauri.main/MD_BASE ""
                               toddler.tauri.main/MD_REFRESH_PERIOD 0
                               toddler.tauri.main/ROUTER_BASE ""
                               toddler.tauri.docs/SEARCH_INDEX ~(str "/docs.index." salt ".edn")}
            :asset-path "/js"))))

(defn build-index
  [{:keys [output root]
    :or {output (str "target/" index-file)
         root "/docs"}}]
  (letfn [(->link [route]
            (str root route))]
    (let [{:keys [mds output]} {:output output
                                :mds [{:route (->link "/intro")
                                       :topic "Rationale"
                                       :path "dev/docs/intro.md"}]}]
      (println "Refreshing index file at " (str output))

      (b/process
       {:command-args ["clj" "-X:index" ":mds" (str mds) ":output" (str output)]
        :out :capture
        :err :capture}))))

(defn release
  [_]
  ;; BUILD CSS
  (b/delete {:path "target"})
  (b/process
   {:command-args ["clj" "-X:shadow:css" "salt" salt]})
  (let [command ["npx" "shadow-cljs" "-A:shadow" "--config-merge" (str (frontend-config)) "release" "release"]]
    (b/process
     {:command-args command
      :out :capture
      :err :capture}))
  (build-index nil)
  (b/copy-dir
   {:src-dirs ["dev/docs"]
    :target-dir "target/docs"})
  (b/delete {:path "target/index.html"})
  (b/delete {:path "target/404.html"})
  (template/process
   "index.html.tmp" "target/index.html" {:salt salt :root ""})
  (template/process
   "index.html.tmp" "target/404.html" {:salt salt :root ""}))
