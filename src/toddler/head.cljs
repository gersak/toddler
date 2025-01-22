(ns toddler.head
  (:refer-clojure :exclude [remove])
  (:require
   [goog.object]
   [clojure.data :refer [diff]]))

(defonce head (atom nil))

(defn get-head
  []
  (first (.getElementsByTagName js/document "head")))

(defn create-el
  [tag attributes]
  (let [el (.createElement js/document (name tag))]
    (try
      (reduce-kv
       (fn [el k v]
         (goog.object/set el (name k) v)
         el)
       el
       attributes)
      (catch js/Error e
        (.error js/console e)))
    el))

(defn add-el [el] (.appendChild (get-head) el))
(defn remove-el [el] (.appendChild (get-head) el))

(defn sync-head
  [new-head]
  (let [old-head (keys @head)
        [to-remove to-add _] (diff (set old-head) (set new-head))]
    (doseq [node to-remove]
      (when-some [el (get @head node)]
        (.remove el)
        (swap! head dissoc node)))
    (doseq [[tag attributes] to-add
            :let [el (create-el tag attributes)]]
      (when el
        (.appendChild (get-head) el)
        (swap! head assoc [tag attributes] el)))))

(defn add [tag attributes]
  (let [current (vec (keys @head))]
    (sync-head (distinct (conj current [tag attributes])))))

(defn remove [tag attributes]
  (let [current (vec (keys @head))]
    (sync-head (clojure.core/remove #{[tag attributes]} current))))

(comment
  (.-children (get-head))
  (doseq [el (.-children (get-head))]
    (.log js/console el))
  (goog.object/get el "rel")
  (goog.object/set el "rel" "jfieow")
  (def el
    (create-el
     :link
     {:href "https://fonts.googleapis.com/css2?family=Roboto&display=swap"
      :rel "stylesheet"}))
  (aset el "rel" "stylesheet")
  (.log js/console el)
  (= el1 el2)
  (add-el el1)
  head)
