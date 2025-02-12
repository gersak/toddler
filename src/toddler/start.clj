(ns toddler.start
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn exists?
  [project]
  (.exists (io/file project)))

(defn build-shell
  [project]
  (assert (not (exists? project)) "Project is already active")
  (io/make-parents (str project "/src/" project "/"))
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
       (spit target new-content)))))

(defn init-files
  [project]
  (assert (exists? project) "Project folder isn't ready")
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
    (process "template/src/main.cljs.tmp"
             (->file "src/main.cljs")
             {:project project})
    (process "template/src/main.css" (->file "src/main.css"))))

(defn -main
  [[project & others]]
  (assert (some? project) "Specify project name")
  (build-shell project)
  (init-files project))

(comment
  (def project "test-project")
  (process "template/package.json" (str project "/package.json") {:project project}))
