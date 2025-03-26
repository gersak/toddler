(ns build
  (:require
   [clojure.edn :as edn]
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]
   [toddler.template :as template]))

(def version "0.9.7")
(def target "target/classes")

(defonce salt (str "b_" (template/random-string)))
(defonce index-file (format "/docs.index.%s.edn" salt))

(defn create-jar []
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir target})
    (b/write-pom {:target target
                  :lib 'dev.gersak/toddler
                  :version version
                  :basis basis})
    (b/jar {:class-dir target
            :jar-file (format "target/toddler-%s.jar" version)})))

(defn deploy
  []
  (let [jar-file (format "target/toddler-%s.jar" version)
        pom-file (str target "/pom.xml")]
    (println "Deploying JAR:" jar-file)
    (dd/deploy {:installer :remote
                :sign-releases? false
                :artifact jar-file
                :pom-file pom-file})))

(defn test-deploy
  []
  (let [jar-file (format "target/toddler-%s.jar" version)
        pom-file (str target "/pom.xml")]
    (println "Deploying JAR:" jar-file)
    (dd/deploy {:installer :local
                :sign-releases? false
                :artifact jar-file
                :pom-file pom-file})))

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
     (assoc :closure-defines `{toddler.showcase/MD_BASE "https://raw.githubusercontent.com/gersak/toddler-showcase/refs/heads/main/docs"
                               toddler.showcase/ROUTER_BASE "toddler"
                               toddler.showcase/SEARCH_INDEX ~(str "/toddler-showcase" index-file)
                               ; toddler.showcase/ROUTER_BASE ""
                               ; toddler.showcase/SEARCH_INDEX ~index-file
                               toddler.showcase/MD_REFRESH_PERIOD 0}
            :asset-path "/toddler-showcase/js"))))

(defn search-index-config
  [root]
  (letfn [(->link [route]
            (str root route))]
    {:output (str "showcase/web" index-file)
     :mds [{:route (->link "/rationale")
            :topic "Rationale"
            :path "showcase/docs/rationale.md"}
           {:route (->link "/calendar")
            :topic "Calendar"
            :path "showcase/docs/calendar.md"}
           {:route (->link "/i18n")
            :topic "i18n"
            :path "showcase/docs/i18n.md"}
           {:route (->link "/icons")
            :topic "Icons"
            :path "showcase/docs/icons.md"}
           {:route (->link "/inputs")
            :topic "Inputs"
            :path "showcase/docs/inputs.md"}
           {:route (->link "/layout")
            :topic "Layout"
            :path "showcase/docs/layout.md"}
           {:route (->link "/modal")
            :topic "Modal"
            :path "showcase/docs/modal.md"}
           {:route (->link "/notifications")
            :topic "Notifications"
            :path "showcase/docs/notifications.md"}
           {:route (->link "/popup")
            :topic "Popup"
            :path "showcase/docs/popup.md"}
           {:route (->link "/rationale")
            :topic "Rationale"
            :path "showcase/docs/rationale.md"}
           {:route (->link "/routing")
            :topic "Routing"
            :path "showcase/docs/routing.md"}
           {:route (->link "/tables")
            :topic "Tables"
            :path "showcase/docs/tables.md"}
           {:route (->link "/tauri")
            :topic "Tauri"
            :path "showcase/docs/tauri.md"}
           {:route (->link "/lazy")
            :topic "Lazy"
            :path "showcase/docs/lazy.md"}]}))

(defn github-release
  [_]
  ;; BUILD CSS
  (println "Cleaining showcase/web")
  (b/delete {:path "showcase/web/"})
  (b/process
   {:command-args ["clj" "-X:shadow:showcase:css" "salt" salt]})
  (let [command ["npx" "shadow-cljs" "-A:shadow:showcase" "--config-merge" (str (frontend-config)) "release" "release"]]
    (b/process
     {:command-args command
      :out :capture
      :err :capture}))
  (println "Creating showcase/web/index.html file")
  (template/process
   "index.html.tmp" "showcase/web/index.html" {:salt salt :root ""})
  (println "Refreshing docs/index.html")
  (b/delete {:path "docs/index.html"})
  (template/process
   "index.html.tmp" "docs/index.html" {:salt salt :root "/toddler-showcase"})
  (println "Refreshing docs/404.html")
  (b/delete {:path "docs/404.html"})
  (template/process
   "index.html.tmp" "docs/404.html" {:salt salt :root "/toddler-showcase"})
  (let [{:keys [mds output]} (search-index-config "/toddler")
        _ (println "Refreshing index file at " (str output))
        {:keys [err]} (b/process
                       {:command-args ["clj" "-X:index" ":mds" (str mds) ":output" (str output)]
                        :out :capture
                        :err :capture})]
    (when (not-empty err)
      (println "[ERROR] " err))))

(defn release
  ([] (release nil))
  ([& _]
   (create-jar)
   (deploy)))

(comment
  (def config-file "shadow-cljs.prod.edn")
  (template/random-string)
  (github-release)
  (release))
