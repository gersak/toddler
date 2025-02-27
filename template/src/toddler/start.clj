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
                          ; (println "Replacing {{" variable "}} in " file)
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
    (process "template/README.md" (->file "README.md"))
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
    (process "template/dev/docs/intro.md"
             (->file "dev/docs/intro.md")
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
      (process "template/src/main.css" (->file (str "src/" dir "/main.css"))))))

; (defn init-tauri
;   [project]
;   (letfn [(->file
;             [path]
;             (str project "/" path))]
;     (process "template/src-tauri/capabilities/default.json" (->file "src-tauri/capabilities/default.json"))
;     (letfn [(process-icon [x]
;               (process (str "template/src-tauri/icons/" x) (->file (str "src-tauri/icons/" x))))]
;       (let [icons ["128x128.png" "128x128@2x.png"
;                    "32x32.png" "icon.icns" "icon.ico"
;                    "icon.png" "Square107x107Logo.png" "Square142x142Logo.png"
;                    "Square150x150Logo.png" "Square284x284Logo.png" "Square30x30Logo.png"
;                    "Square310x310Logo.png" "Square44x44Logo.png" "Square71x71Logo.png"
;                    "Square89x89Logo.png" "StoreLogo.png"]]
;         (doseq [icon icons]
;           (process-icon icon))))
;     (process "template/src-tauri/src/lib.rs" (->file "src-tauri/src/lib.rs"))
;     (process "template/src-tauri/src/main.rs" (->file "src-tauri/src/main.rs"))
;     (process "template/src-tauri/build.rs" (->file "src-tauri/build.rs"))
;     (process "template/src-tauri/Cargo.toml" (->file "src-tauri/Cargo.toml"))
;     (process "template/src-tauri/tauri.conf.json"
;              (->file "src-tauri/tauri.conf.json")
;              {:project project})))

(defn init-tauri
  [project]
  (sh "npx" "tauri" "init" "-A" project "-W" project
      "-D" "../dev"
      "--dev-url" "http://localhost:8000"
      "--before-dev-command" "npm run dev"
      "--before-build-command" "npm run release"
      :dir project))

(defn install-js
  [project]
  (sh "npm" "install" :dir project))

(defn -main
  [& args]
  (let [[project] args]
    (assert (some? project) "Specify project name")
    (init-files project)
    (install-js project)
    (try
      (init-tauri project)
      (catch Throwable _ nil))
    (System/exit 0)))

(comment
  (def project "test-project")
  (init-tauri project)
  (process "template/package.json" (str project "/package.json") {:project project})
  (-main [project]))
