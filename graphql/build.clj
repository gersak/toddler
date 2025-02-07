(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version "0.2.0")
(def target "target/classes")

(defn create-jar []
  (let [basis (b/create-basis {})]
    (b/delete {:path "target"})
    (b/copy-dir {:src-dirs ["src"]
                 :target-dir target})
    (b/write-pom {:target target
                  :lib 'dev.gersak/toddler-graphql
                  :version version
                  :basis basis})
    (b/jar {:class-dir target
            :jar-file (format "target/toddler-graphql-%s.jar" version)})))

(defn deploy
  []
  (let [jar-file (format "target/toddler-graphql-%s.jar" version)
        pom-file (str target "/pom.xml")]
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
