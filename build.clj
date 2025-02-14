(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version "1.0.0")
(def target "target/classes")

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
