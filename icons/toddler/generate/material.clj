(ns toddler.generate.material
  (:require
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    [babashka.fs :as fs]
    [clojure.xml :as xml]
    [clojure.java.shell :as sh]
    [clojure.java.io :as io]))


(def root "icons/.repos")


(defn ensure-root
  []
  (io/make-parents "icons/.repos/README.md"))


(defn clone-repo
  []
  (sh/sh
    "git" "clone" "https://github.com/google/material-design-icons.git"
    (str root "/" "material")))


(defn clean-repo
  []
  (sh/sh "rm" "-r" (str root "/material")))


(defn analyze-material-path
  [p]
  (let [[size style name] (reverse (str/split (str p) #"/"))]
    {:name (str/replace name #"_+" "-")
     :style (case style
              "materialiconsoutlined" "material-icons-outlined"
              "materialiconsround" "material-icons-round"
              "materialicons" "material-icons"
              "materialiconssharp" "material-icons-sharp"
              "materialiconstwotone" "material-icons-two-tone")
     :size (case size
             "24px.svg" 24
             "20px.svg" 20
             "36px.svg" 36
             "18px.svg")
     :file (str p)}))


(defn analyze-svgs
  []
  (let [a (volatile! nil)]
    (fs/walk-file-tree
      (str root "/material/src")
      {:visit-file
       (fn [f _]
         (let [{:keys [style] :as data} (analyze-material-path f)]
           (vswap! a update style (fnil conj []) data))
         :continue)})
    @a))



(comment
  (keys all-material)

  (def all-material
    (analyze-svgs)))


(defn gen-el
  [{:keys [tag attrs content]}]
  (let [attrs (cond->
                attrs
                (= tag :svg)
                (assoc 
                  :height "1em"
                  :width "1em"
                  :stroke "currentColor"
                  :fill "currentColor")
                ;;
                (#{:line :path :polyline :rect :circle :polygon} tag)
                (as-> tag
                  (update tag :stroke (fn [v] (if (= v "#000") "currentColor" v)))
                  (update tag :fill (fn [v] (if (= v "#000") "currentColor" v))))
                ;;
                (some? (:style attrs))
                (update :style (fn [current]
                                 (reduce
                                   (fn [r e]
                                     (let [[k v] (str/split e #":")]
                                       (if (#{"stroke" "fill"} k)
                                         (case v
                                           "none" (assoc r (keyword k) "none")
                                           "#000" r)
                                         (assoc r (keyword k) v))))
                                   nil
                                   (when current (str/split current #";"))))))]
    (cond
      (= :title tag)
      `(~(symbol "helix.dom/title") ~attrs)
      ;;
      (empty? content)
      `(~(symbol "helix.dom" (name tag))
                 ~(cond-> attrs
                    (= tag :svg) (assoc :& 'props)))
      :else
      `(~(symbol "helix.dom" (name tag))
                 ~(cond-> attrs
                    (= tag :svg) (assoc :& 'props))
                 ~@(map gen-el content)))))


(defn generate-icon
  [{:keys [file]
    size :size
    icon :name}]
  (when (= 24 size)
    (let [xml (xml/parse file)
          icon (if (re-find #"^\d" (name icon))
                 (str "_" (name icon))
                 (name icon))]
      `(helix.core/defnc ~(symbol icon) [~'props] ~(gen-el xml)))))


(defn prepare-output
  [all-material]
  (let [all-material (analyze-svgs)]
    (reduce-kv
      (fn [clojure target icons]
        (let [[ns file]
              (case target
                "material-icons" ["toddler.material" "material.cljc"]
                "material-icons-outlined" ["toddler.material.outlined" "material/outlined.cljc"]
                "material-icons-two-tone" ["toddler.material.two-tone" "material/two_tone.cljc"]
                "material-icons-sharp" ["toddler.material.sharp" "material/sharp.cljc"]
                "material-icons-round" ["toddler.material.round" "material/round.cljc"])]
          (assoc clojure
                 file
                 (into
                   [`(~'ns ~(symbol ns) 
                       ~(case target
                          `(:refer-clojure
                             :exclude [~'merge ~'map
                                       ~'loop ~'cast ~'repeat
                                       ~'shuffle ~'sync ~'remove
                                       ~'sort ~'send ~'compare ~'filter
                                       ~'update ~'print ~'list ~'commute
                                       ~'class ~'comment ~'key]))
                       (:require
                         [~'helix.core]
                         [~'helix.dom]))]
                   (keep generate-icon icons)))))
      nil
      all-material)))


(defn generate
  []
  (let [output (prepare-output (analyze-svgs))]
    (doseq [[file declaration] output
            :let [target (str "gen/toddler/" file)]]
      (println "Generating " target)
      (spit
        target
        (str/join
          "\n\n"
          (map #(with-out-str (pprint %)) declaration))))))


(comment
  (def icon (-> all-material first val first))
  (xml/parse (:file icon))
  (generate-icon icon)
  (def a (analyze-svgs))
  (keys a)
  (def output (prepare-output (analyze-svgs)))
  (let [[file declaration] (nth (seq output) 3)]
    (def file file)
    (def declaration declaration)
    (def target (str "gen/toddler/" file)))
  (spit "error.edn" (with-out-str (pprint (analyze-svgs))))
  (-> output keys)

  (keys output))
