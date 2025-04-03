(ns toddler.start
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.java.shell :refer [sh]]
   [toddler.template :refer [process copy]]))

(defn exists?
  [project]
  (.exists (io/file project)))

(defn build-shell
  [project]
  (assert (not (exists? project)) "Project is already active")
  (io/make-parents (str project "/src/" project "/main.cljs"))
  (io/make-parents (str project "/dev/"))
  (io/make-parents (str project "/build/")))

(defn init-files
  [project]
  (letfn [(->file
            [path]
            (str project "/" path))]
    (process "template/README.md" (->file "README.md"))
    (process "template/package.json.tmp"
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
    (process "template/dev/docs/intro.md"
             (->file "dev/docs/intro.md")
             {:project project})
    (copy "template/build/index.html.tmp"
          (->file "build/index.html.tmp"))
    (process "template/build.clj.tmp"
             (->file "build.clj")
             {:project project})
    (let [dir (->
               project
               str/lower-case
               (str/replace #"-+" "_")
               (str/replace #"\." "/"))]
      (process "template/src/main.cljs.tmp"
               (->file (str "src/" dir "/main.cljs"))
               {:project project
                :project-folder dir})
      (process "template/dev/docs/greeting.md"
               (->file "dev/docs/greeting.md")
               {:project-folder dir})
      (process "template/src/docs.cljs.tmp"
               (->file (str "src/" dir "/docs.cljs"))
               {:project project})
      (process "template/src/docs/lazy.cljs.tmp"
               (->file (str "src/" dir "/docs/lazy.cljs"))
               {:project project})
      (process "template/src/main.css" (->file (str "src/" dir "/main.css"))))))

(defn init-tauri
  [project]
  (sh "npx" "tauri" "init" "-A" project "-W" project
      "-D" "../dev"
      "--dev-url" "http://localhost:8000"
      "--before-dev-command" "npm run dev"
      "--before-build-command" "npm run release"
      :dir project))

(def windows? (boolean (re-find #"(?i)windows" (System/getProperty "os.name"))))

(defn install-js
  [project]
  (if windows?
    (sh "npm.cmd" "install" :dir project)
    (sh "npm" "install" :dir project)))

(defn -main
  [& args]
  (let [[project] args]
    (assert (some? project) "Specify project name. I.E. 'new.project'")
    (init-files project)
    (install-js project)
    (try
      (init-tauri project)
      (catch Throwable _ nil))
    (System/exit 0)))

(comment
  (def project "test-project")
  (init-tauri project)
  (process "template/package.json.tmp" (str project "/package.json") {:project project})
  (-main [project]))
