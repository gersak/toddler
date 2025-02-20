(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

;; IMPORTANT!!!! - change version in resources/template/deps.edn.tmp
;; to match this version
(def version "0.9.7-SNAPSHOT")
(def target "target/classes")

(defn create-jar []
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir target})
    (b/write-pom {:target target
                  :lib 'dev.gersak/toddler-ui
                  :version version
                  :basis basis
                  :pom-data [[:licenses
                              [:license
                               [:name "MIT"]
                               [:url "https://opensource.org/licenses/MIT"]
                               [:distribution "repo"]]]]})
    (b/jar {:class-dir target
            :jar-file (format "target/ui-%s.jar" version)})))

(defn deploy
  []
  (let [jar-file (format "target/ui-%s.jar" version)
        pom-file (str target "/pom.xml")]
    (println "Deploying JAR:" jar-file)
    (dd/deploy {:installer :remote
                :sign-releases? false
                :artifact jar-file
                :pom-file pom-file})))

(defn generate-shadow-indexes
  []
  (b/process
   {:command-args ["clj" "-X:release"]
    :dir "."}))

(defn release
  ([] (release nil))
  ([& _]
   (generate-shadow-indexes)
   (create-jar)
   (deploy)))

(comment
  (release))
