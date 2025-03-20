(ns toddler.search
  (:require
   [toddler.search.en :as en]
   #?@(:cljs
       [[toddler.core :as toddler]
        [helix.hooks :as hooks]
        [helix.core :refer [$ defnc fnc create-context provider defhook]]
        [helix.children :refer [children]]
        [toddler.search.context :as search.context]
        [clojure.core.async :as async]
        [clojure.edn :as edn]])))

(def ^:dynamic *stop-words* en/stop-words)
(def ^:dynamic *stem* en/stem)
(def ^:dynamic *record* nil)
(def ^:dynamic *field* nil)
(def ^:dynamic *index* nil)
(def ^:dynamic *weights* nil)

(comment
  (contains? en/stop-words ""))

(defn levenshtein
  "Compute the Levenshtein distance between two strings."
  [s1 s2]
  (let [len1 (count s1)
        len2 (count s2)
        matrix (vec (map vec (repeat (inc len1) (range (inc len2)))))]

    (loop [i 1 matrix matrix]
      (if (> i len1)
        (get-in matrix [len1 len2])
        (recur (inc i)
               (loop [j 1 m matrix]
                 (if (> j len2)
                   m
                   (recur (inc j)
                          (assoc-in m [i j]
                                    (min (inc (get-in m [(dec i) j]))   ;; Deletion
                                         (inc (get-in m [i (dec j)]))   ;; Insertion
                                         (+ (get-in m [(dec i) (dec j)])
                                            (if (= (nth s1 (dec i)) (nth s2 (dec j))) 0 1))))))))))))

(defn tokenize
  [source]
  (re-seq #"\w+" source))

(defn index-words
  ([source] (index-words nil source))
  ([result source]
   (if (nil? source)
     result
     (let [words (tokenize source)
           indexed-words (map-indexed
                          (fn [idx v]
                            [idx v])
                          words)]
       (letfn [(process
                 [result [idx word]]
                 (if (contains? *stop-words* word)
                   result
                   (let [stemed (*stem* word)]
                     (update-in result [:word stemed]
                                (fn [current]
                                  (update-in current [*record* ::index *field*]
                                             (fn [data]
                                               (->
                                                data
                                                (update :tf (fnil inc 0))
                                                (update :position (fnil conj []) idx)))))))))]
         ; (assoc-in result [:document *record* *field* :wc] (count words))
         (assoc-in result [:document *record* ::index *field* :wc] 100)
         (as-> (assoc-in result [:document *record* ::index *field* :wc] (count words)) index
           (reduce process index indexed-words)))))))

(defn build-index
  ([data]
   (letfn [(init-index []
             {:fields (reduce into #{} (map (comp keys :index) data))
              :document (reduce
                         (fn [docs {m :meta
                                    r :ref}]
                           (if m
                             (assoc docs r m)
                             m))
                         nil
                         data)})
           (process [index {id :ref document-index :index}]
             (reduce-kv
              (fn [index field value]
                (binding [*record* id
                          *field* field]
                  (index-words index value)))
              index
              document-index))
           (count-documents
             [index]
             (update index :document
                     (fn [document]
                       (assoc document ::count (count document)))))
           (document-frequencies [index word documents]
             (assoc-in index [word :df] (count documents)))]
     (as-> (init-index) index
       (reduce process index data)
       (count-documents index)
       (update index :word #(reduce-kv document-frequencies % %))
       (update index :document (fn [documents]
                                 (as-> documents documents
                                   (reduce
                                    (fn [documents field]
                                      (assoc-in documents
                                                [::wc-avg field]
                                                (apply + (remove nil? (map (comp :wc field ::index) (vals documents))))))
                                    documents
                                    (:fields index))
                                   (let [document-count (::count documents)]
                                     (update documents ::wc-avg
                                             (fn [avg]
                                               (reduce-kv
                                                (fn [result field all]
                                                  (if (nil? all)
                                                    result
                                                    (assoc result field (/ all document-count))))
                                                avg
                                                avg)))))))))))

(def k 1.2) ;; BM25 tuning parameter
(def b 0.75) ;; BM25 tuning parameter

(defn b25-score
  [tf idf doc-length doc-avg]
  (let [;doc-length (get-in documents [id field :wc] 0)
        denominator (+ tf (* k (+ 1 (- b) (* b (/ doc-length doc-avg)))))]
    (* idf (/ (* tf (+ 1 k)) denominator))))

(defn search
  ([query] (search query *index*))
  ([query {{avg-fields ::wc-avg :as documents} :document :as index}]
   (letfn [(find-word-index [word]
             (get-in index [:word word]))
           (rank-word [word-index]
             (if (nil? word-index) nil
                 (let [{:keys [df]} word-index
                       process-documents (dissoc word-index :df)
                       idf (Math/log (+ 1 (/ (- (::count documents 0) df) (+ df 0.5))))
                       results  (reduce-kv
                                 (fn [ratings id {fields ::index}]
                                   (reduce-kv
                                    (fn [ratings field {:keys [tf]}]
                                      (let [doc-length (get-in documents [id ::index field :wc] 0)
                                            doc-avg (get avg-fields field)
                                            score (b25-score tf idf doc-length doc-avg)]
                                        (assoc ratings [id field] score)))
                                    ratings
                                    fields))
                                 nil
                                 process-documents)]
                   results)))
           (rank-words [words]
             (map
              (fn [word]
                {:word word
                 :ranking (rank-word (find-word-index (*stem* word)))})
              words))
           (apply-weights
             [ranking]
             (when ranking
               (mapv
                (fn [{:keys [ranking] :as data}]
                  (assoc data :ranking
                         (reduce-kv
                          (fn [ranking [_ field :as k] score]
                            (if-let [mult (get *weights* field)]
                              (assoc ranking k (* score mult))
                              ranking))
                          ranking
                          ranking)))
                ranking)))
           (sum-rankings
             [ranking]
             (when ranking (apply merge-with + (map :ranking ranking))))
           (sort-results
             [result]
             (when result (sort-by val > result)))
           (return-docs
             [results]
             (distinct
              (map
               (fn [[[_ref]]]
                 (get-in index [:document _ref]))
               results)))]
     (-> query tokenize
         rank-words
         apply-weights
         sum-rankings
         sort-results
         return-docs))))

#?(:cljs (def -index- (create-context)))

#?(:cljs
   (defn load-index
     [path]
     (async/go
       (if-let [index (async/<! (toddler/fetch path))]
         (try
           (let [result (clojure.edn/read-string index)]
             (if (map? result) result
                 (do
                   (.error js/console (str "Couldn't load index from: " path))
                   :nil)))
           (catch js/Error _
             (.error js/console (str "Couldn't load index from: " path))
             :nil))
         :nil))))

#?(:cljs
   (defnc Provider
     [{:keys [path] :as props}]
     (let [[index set-index!] (hooks/use-state nil)
           base (hooks/use-context search.context/base)]
       (hooks/use-effect
         :once
         (async/go
           (let [full-path (str (or base "") path)
                 index (async/<! (load-index full-path))]
             (when (not= :nil index)
               (set-index! index)))))
       (provider
        {:context -index-
         :value index}
        (children props)))))

#?(:cljs
   (defn wrap-index
     ([component path]
      (fnc [props]
        ($ Provider {:path path} ($ component {& props}))))))

#?(:cljs
   (defhook use-results
     [{:keys [value]}]
     (let [index (hooks/use-context -index-)
           _value (toddler/use-delayed value)
           [results set-results!] (hooks/use-state nil)]
       (hooks/use-effect
         [_value]
         (if (empty? _value)
           (set-results! nil)
           (set-results! (search _value index))))
       results)))

(comment
  (def query "hook")
  (def query "positive notification")
  (*stem* "notify")
  (*stem* "notification")
  (*stem* "notifications")
  (def index (time (toddler.search.docs/build-index toddler.search.docs/config)))
  (binding [*weights* {:content 0.7 :title 1.8}]
    (time (search "toddler route" index)))
  (get-in index [:word "hook"])
  (-> index :document)
  (def index nil)
  (build-index *1)
  (alter-var-root
   #'*index*
   (fn [_] (build-index config)))
  (keys (:word index))
  (-> config :mds prepare-index)
  (contains? *stop-words* "")
  (count (keys index))
  (-> index :document :count)
  (-> index :word (get "hook")))
