(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version "0.1.0")
(def target "target")

(defn create-jar []
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})
    (b/copy-dir {:src-dirs ["default"]
                 :target-dir target})
    (b/write-pom {:target target
                  :lib 'com.github.gersak/toddler-theme
                  :version version
                  :basis basis
                  :pom-data [[:licenses
                              [:license
                               [:name "MIT"]
                               [:url "https://opensource.org/licenses/MIT"]
                               [:distribution "repo"]]]]})
    (b/jar {:class-dir target
            :jar-file (format "target/theme-%s.jar" version)})))

(defn deploy
  []
  (let [jar-file (format "target/theme-%s.jar" version)
        pom-file (str target "/pom.xml")]
    (println "Deploying JAR:" jar-file)
    (dd/deploy {:installer :remote
                :sign-releases? false
                :artifact jar-file
                :pom-file pom-file})))

(defn release
  ([] (release nil))
  ([& _]
   (create-jar)
   (deploy)))

(comment
  (release))
