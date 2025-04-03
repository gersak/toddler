(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version "0.5.4")
(def target "target/classes")

(defn create-jar []
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})
    (b/copy-dir {:src-dirs ["src"]
                 :target-dir target})
    (b/write-pom {:target target
                  :lib 'dev.gersak/toddler-template
                  :version version
                  :basis basis
                  :pom-data [[:licenses
                              [:license
                               [:name "MIT"]
                               [:url "https://opensource.org/licenses/MIT"]
                               [:distribution "repo"]]]]})
    (b/jar {:class-dir target
            :jar-file (format "target/template-%s.jar" version)})))

(defn release
  ([] (release nil))
  ([{t :test}]
   (create-jar)
   (let [jar-file (format "target/template-%s.jar" version)
         pom-file (str target "/pom.xml")
         installer (if t :local :remote)]
     (println "Installing JAR " (name installer))
     (dd/deploy {:installer installer
                 :sign-releases? false
                 :artifact jar-file
                 :pom-file pom-file}))))

(comment
  (release))

; (letfn [(->link [route]
;           (str "/docs" route))]
;   (def mds
;     [{:route (->link "/intro")
;       :topic "Rationale"
;       :path "dev/docs/intro.md"}]))
;
; (defn release
;   [_]
;   ;; BUILD CSS
;   (b/delete {:path "target"})
;   (comment
;     (template/run-script (str "clj -X:shadow:css salt " salt)))
;   ;; Working
;   (b/process {:command-args ["powershell" "-NoProfile" "-Command" (str "clj -X:shadow:css salt " salt)]})
;   (b/process {:command-args ["Invoke-Clojure" "-X:shadow:css" "salt" salt]})
;   (let [{:keys [err]} (template/run-script
;                        (str "clj -X:shadow:css salt " salt)
;                        (str "npx shadow-cljs --config-merge "
;                             (template/escape-data (frontend-config))
;                             " release release")
;                        (str "clj -X:index :mds " (template/escape-data mds) :output (str "target/" index-file)))])
;   (b/copy-dir
;    {:src-dirs ["dev/docs"]
;     :target-dir "target/docs"})
;   (b/delete {:path "target/index.html"})
;   (b/delete {:path "target/404.html"})
;   (template/process
;    "index.html.tmp" "target/index.html" {:salt salt :root ""})
;   (template/process
;    "index.html.tmp" "target/404.html" {:salt salt :root ""}))
