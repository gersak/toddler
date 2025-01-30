Currently Toddler generates components in following namespaces:

 * **toddler.material.outlined**
 * **toddler.material.round**
 * **toddler.material.sharp**
 * **toddler.fav6.regular**
 * **toddler.fav6.solid**
 * **toddler.fav6.brands**
 * **toddler.ionic**


 Here is example of generating FontAwesome icons

```clojure
(ns toddler.generate.fav6
  (:require
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    [babashka.fs :as fs]
    [clojure.xml :as xml]
    [clojure.java.shell :as sh]
    [clojure.java.io :as io]))


(def root ".icons/")


(defn ensure-root
  []
  (io/make-parents ".icons/README.md"))


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
  (generate))
```
