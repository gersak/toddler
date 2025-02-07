(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version "0.1.0-SNAPSHOT")
(def target "target/classes")

(defn create-jar []
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})
    (b/copy-dir {:src-dirs ["src"]
                 :target-dir target})
    (b/write-pom {:target target
                  :lib 'com.github.gersak/toddler-ui
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
