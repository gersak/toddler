(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version "0.5.2")
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
