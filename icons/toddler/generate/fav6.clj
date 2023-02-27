(ns toddler.generate.fav6
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
    "git" "clone" "https://github.com/FortAwesome/Font-Awesome.git"
    (str root "/" "fav6")))


(defn clean-repo
  []
  (sh/sh "rm" "-r" (str root "/fav6")))


(defn list-images
  [style]
  (fs/list-dir (str root "/fav6/svgs/" style)))


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
                (some? (:style attrs))
                (update :style (fn [current]
                                 (reduce
                                   (fn [r e]
                                     (let [[k v] (str/split e #":")]
                                       (if (#{"stroke" "fill"} k)
                                         (assoc r (keyword k) "currentColor")
                                         (assoc r (keyword k) v))))
                                   nil
                                   (when current (str/split current #";"))))))]
    (if (empty? content)
      `(~(symbol "helix.dom" (name tag))
                 ~(cond-> attrs
                    (= tag :svg) (assoc :& 'props)))
      `(~(symbol "helix.dom" (name tag))
                 ~(cond-> attrs
                    (= tag :svg) (assoc :& 'props))
                 ~@(map gen-el content)))))


(defn process-svg
  [path]
  (let [xml (xml/parse (str path))
        [icon] (fs/split-ext (fs/file-name path))
        icon (if (re-find #"^\d" (name icon))
               (str "_" (name icon))
               (name icon))]
    `(helix.core/defnc ~(symbol icon) [~'props] ~(gen-el xml))))


(defn generate-fa
  [style]
  (str/join
    "\n\n"
    (map
      ; str
      #(with-out-str (pprint %))
      (reduce
        (fn [r path]
          (conj r (process-svg path)))
        [`(~'ns ~(symbol (str "toddler.fav6." style)) 
            ~(case style
               "brands" `(:refer-clojure
                          :exclude [~'meta])
               "regular" `(:refer-clojure
                            :exclude [~'map ~'comment ~'clone])
               "solid" `(:refer-clojure
                          :exclude [~'map ~'clone ~'comment ~'list
                                    ~'repeat ~'divide ~'key ~'mask
                                    ~'filter ~'shuffle ~'atom ~'cat
                                    ~'print ~'sort]))
            
            (:require
              [~'helix.core]
              [~'helix.dom]))]
        (list-images style)))))


(defn generate
  []
  (let [styles ["regular" "brands" "solid"]]
    (doseq [style styles]
      (spit
        (str "gen/toddler/fav6/" style ".cljs")
        (generate-fa style)))))


(comment
  (def x (generate-fa "brands"))
  (generate)
  (clone-repo)
  (def style "regular")
  (def path (first (list-images style)))
  (def target "io5"))
