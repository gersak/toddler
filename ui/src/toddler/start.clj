(ns toddler.start
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.shell :refer [sh]]))

(defn exists?
  [project]
  (.exists (io/file project)))

(defn build-shell
  [project]
  (assert (not (exists? project)) "Project is already active")
  (io/make-parents (str project "/src/" project "/main.cljs"))
  (io/make-parents (str project "/dev/")))

(defn process
  ([file target] (process file target nil))
  ([file target variables]
   ; (def file "template/package.json")
   ; (def variables {:project project})
   (let [content (io/resource file)]
     (assert (some? content) "File doesn't exist!")
     (let [content (slurp content)
           _variables (re-seq #"(?<=\{\{).*(?=\}\})" content)
           new-content (reduce
                        (fn [result variable]
                          (str/replace result
                                       (re-pattern (str "\\{\\{" variable "\\}\\}"))
                                       (get variables (keyword variable))))
                        content
                        _variables)]
       (io/make-parents target)
       (when (.exists (io/file target))
         (throw
          (ex-info "File already exists"
                   {:template file
                    :target target})))
       (spit target new-content)))))

(defn init-files
  [project]
  (letfn [(->file
            [path]
            (str project "/" path))]
    (process "template/package.json"
             (->file "package.json")
             {:project project})
    (process "template/shadow-cljs.edn.tmp"
             (->file "shadow-cljs.edn")
             {:project project})
    (process "template/deps.edn.tmp" (->file "deps.edn"))
    (process "template/.gitignore" (->file ".gitignore"))
    (process "template/dev/compile_css.clj.tmp"
             (->file "dev/compile_css.clj")
             {:project project})
    (process "template/dev/index.html" (->file "dev/index.html"))
    (process "template/dev/user.clj.tmp" (->file "dev/user.clj"))
    (let [dir (->
               project
               str/lower-case
               (str/replace #"-+" "_")
               (str/replace #"\." "/"))]
      (process "template/src/main.cljs.tmp"
               (->file (str "src/" dir "/main.cljs"))
               {:project project
                :project-folder dir})
      (process "template/src/main.css" (->file (str "src/" dir "/main.css"))))))

(defn install-js
  [project]
  (sh "npm" "install" :dir project))

(defn -main
  [& args]
  (let [[project] args]
    (assert (some? project) "Specify project name")
    (init-files project)
    (install-js project)
    (System/exit 0)))

(comment
  (def project "test-project")
  (process "template/package.json" (str project "/package.json") {:project project})
  (-main [project]))
