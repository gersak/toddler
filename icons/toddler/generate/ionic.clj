(ns toddler.generate.ionic
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


; (defn clone-repo
;   [target url]
;   (sh/sh "git" "clone" url (str root "/" target)))

(defn clone-repo
  []
  (sh/sh "git" "clone" "https://github.com/ionic-team/ionicons.git" (str root "/io5")))


(defn list-images
  []
  (fs/list-dir (str root "/io5/src/svg")))


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
        [icon] (fs/split-ext (fs/file-name path))]
    `(helix.core/defnc ~(symbol icon) [~'props] ~(gen-el xml))))


(defn generate-ionic
  []
  (str/join
    "\n\n"
    (map
      ; str
      #(with-out-str (pprint %))
      (reduce
        (fn [r path]
          (conj r (process-svg path)))
        [`(~'ns ~'toddler.ionic
            (:refer-clojure
              :exclude [~'list ~'repeat ~'map ~'key ~'time
                        ~'remove ~'filter ~'shuffle ~'print])
            (:require
              [~'helix.core]
              [~'helix.dom]))]
        (list-images)))))


(defn generate []
  (spit "gen/toddler/ionic.cljs" (generate-ionic)))


(comment
  (clone-repo)
  (generate))
