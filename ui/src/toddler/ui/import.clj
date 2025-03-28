(ns toddler.ui.import
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]))

(def files
  ["toddler/ui/elements.cljs"
   "toddler/ui/elements/calendar.cljs"
   "toddler/ui/elements/modal.cljs"
   "toddler/ui/fields.cljs"
   "toddler/ui/tables.cljs"
   "toddler/ui/components.cljs"])

(defn init
  []
  (println
   (str/replace
    (slurp (io/resource (first files)))
    #"toddler.ui" "new.project.ui")))

(defn file-exists?
  [])

(def component-usage
  "
  Hi, this is utility function for importing Toddler
  components into you codebase. Running this function
  will copy default implementation of UI components to
  ':dir' specified and replace all 'toddler' occurences
  with ':ns' value.
  
  I.E. if you have project that has source code in src
  and you wan't to initialize components at namespace
  'new.project' than running:
  
  clj -X :ns new.project :dir src ;; if dev.gersak/toddler-ui is on classpath

  or

  clj -X {dev.gersak/toddler-ui {:mvn/version \"x.x.x\"}} :ns new.project :dir src

  if dev.gersak/toddler-ui is not on classpath
  
  would copy files:

  toddler/ui/elements.cljs
  toddler/ui/elements/calendar.cljs
  toddler/ui/elements/modal.cljs
  toddler/ui/fields.cljs
  toddler/ui/tables.cljs
  toddler/ui/components.cljs

  to

  new/project/ui/elements.cljs
  new/project/ui/elements/calendar.cljs
  new/project/ui/elements/modal.cljs
  new/project/ui/fields.cljs
  new/project/ui/tables.cljs
  new/project/ui/components.cljs

  From there you can start customizing UI components")

(defn components
  [{:keys [dir]
    _ns :ns
    :or {dir "src"}}]
  (let [dir (str dir)
        _ns (str _ns)]
    (when (empty? _ns)
      (println component-usage)
      (System/exit 1))
    (letfn [(ns->path [_ns]
              (str dir "/" (str/replace _ns #"\." "/") ".cljs"))]
      (doseq [file files
              :let [content (slurp (io/resource file))
                    content' (str/replace content #"toddler\.ui(?=[^\s])" (str _ns ".ui"))
                    [_ _ file-namespace] (re-find #"(\(\s*ns\s+)([\w\.]+)\s*" content')
                    out (ns->path file-namespace)]]
        (when (.exists (io/file out))
          (throw
           (ex-info "File already exists"
                    {:template file
                     :target out})))
        (io/make-parents out)
        (spit out content')))))

(comment
  (str/replace "toddler.ui.elements  " #"toddler\.ui(?=[^\s])" "new.project")
  (str/replace "toddler.ui " #"toddler\.ui(?=[^\s])" "new.project"))
