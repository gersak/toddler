(ns toddler.search
  (:require
   [clojure.string :as str]
   [toddler.search.en :as en]))

(def ^:dynamic *stop-words* en/stop-words)
(def ^:dynamic *stem* en/stem)
(def ^:dynamic *record* nil)
(def ^:dynamic *field* nil)
(def ^:dynamic *index* nil)
(def ^:dynamic *weights* nil)

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
                                  (update-in current [*record* *field*]
                                             (fn [data]
                                               (->
                                                data
                                                (update :tf (fnil inc 0))
                                                (update :position (fnil conj []) idx)))))))))]
         (as-> (assoc-in result [:document *record* *field* :wc] (count words)) index
           (reduce process index indexed-words)))))))

(defn build-index
  ([config] (build-index config [:title :content]))
  ([data fields]
   (letfn [(process [index {:keys [id] :as prepared}]
             (reduce
              (fn [index field]
                (binding [*record* id
                          *field* field]
                  (index-words index (get prepared field))))
              index
              fields))
           (count-documents
             [index]
             (update index :document
                     (fn [document]
                       (assoc document ::count (count document)))))
           (document-frequencies [index word documents]
             (assoc-in index [word :df] (count documents)))]
     (as-> {:fields fields} index
       (reduce process index data)
       (count-documents index)
       (update index :word #(reduce-kv document-frequencies % %))
       (update index :document (fn [documents]
                                 (as-> documents documents
                                   (reduce
                                    (fn [documents field]
                                      (assoc-in documents
                                                [::wc-avg field]
                                                (apply + (remove nil? (map (comp :wc field) (vals documents))))))
                                    documents
                                    fields)
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
             (if (nil? word-index) 0
                 (let [{:keys [df]} word-index
                       process-documents (dissoc word-index :df)
                       idf (Math/log (+ 1 (/ (- (::count documents 0) df) (+ df 0.5))))
                       results  (reduce-kv
                                 (fn [ratings id fields]
                                   (reduce-kv
                                    (fn [ratings field {:keys [tf]}]
                                      (let [doc-length (get-in documents [id field :wc] 0)
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
              ranking))
           (sum-rankings
             [ranking]
             (apply merge-with + (map :ranking ranking)))
           (sort-results
             [result]
             ((sort-by val > result)))]
     (-> query tokenize
         rank-words
         apply-weights
         sum-rankings
         sort-results))))

(comment
  (def query "hook")
  (def query "positive notification")
  (*stem* "notify")
  (*stem* "notification")
  (*stem* "notifications")
  (binding [*weights* {:content 0.7 :title 1.8}]
    (time (search "positive notification" index)))
  (get-in index [:word "hook"])
  (-> index :document)
  (def index nil)
  (def index (time (build-index config)))
  (alter-var-root
   #'*index*
   (fn [_] (build-index config)))
  (keys (:word index))
  (-> config :mds prepare-index)
  (contains? *stop-words* "")
  (count (keys index))
  (-> index :document :count)
  (-> index :word (get "hook")))
