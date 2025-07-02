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
(defonce ^:dynamic *index* nil)
(def ^:dynamic *weights* nil)

(comment
  (contains? en/stop-words ""))

(defn levenshtein
  "Compute the levenshtein distance between two [sequences]."
  [sequence1 sequence2]
  (letfn [(next-row
            [previous current other-seq]
            (reduce
             (fn [row [diagonal above other]]
               (let [update-val (if (= other current)
                                  diagonal
                                  (inc (min diagonal above (peek row))))]
                 (conj row update-val)))
             [(inc (first previous))]
             (map vector previous (next previous) other-seq)))]
    (cond
      (and (empty? sequence1) (empty? sequence2)) 0
      (empty? sequence1) (count sequence2)
      (empty? sequence2) (count sequence1)
      :else (peek
             (reduce (fn [previous current] (next-row previous current sequence2))
                     (map #(identity %2) (cons nil sequence2) (range))
                     sequence1)))))

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
                                    r :ref
                                    idx :index}]
                           (if m
                             (assoc docs r (assoc m
                                             ::raw idx ; Store original content
                                             ::index {})) ; Will be filled with positions
                             docs))
                         {}
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
  (let [denominator (+ tf (* k (+ 1 (- b) (* b (/ doc-length doc-avg)))))]
    (* idf (/ (* tf (+ 1 k)) denominator))))

(defn find-next-position
  "Find the next position within max-distance"
  [positions start-pos max-distance]
  (first (filter #(and (>= % start-pos)
                       (<= % (+ start-pos max-distance)))
                 (sort positions))))

(defn find-sequential-positions
  "Find positions where words appear in sequence within max-distance"
  [word-pos-maps max-distance]
  (when (> (count word-pos-maps) 1)
    (let [[first-word first-positions] (first word-pos-maps)
          remaining-words (rest word-pos-maps)]
      (reduce
       (fn [valid-sequences [word positions]]
         (keep
          (fn [start-pos]
            (when-let [next-pos (find-next-position positions (inc start-pos) max-distance)]
              next-pos))
          valid-sequences))
       (seq first-positions)
       remaining-words))))

(defn find-phrase-matches
  "Find documents where words appear in sequence with proximity matching"
  [word-positions max-distance]
  (when (and (seq word-positions) (> (count word-positions) 1))
    (let [sorted-words (sort-by (comp first :positions) word-positions)]
      (reduce
       (fn [results [doc-id field-positions]]
         (let [word-pos-maps (map (fn [{:keys [word positions]}]
                                    [word (set positions)])
                                  field-positions)
               phrase-matches (find-sequential-positions word-pos-maps max-distance)]
           (if (seq phrase-matches)
             (assoc results [doc-id (first (keys field-positions))]
                    {:phrase-matches (count phrase-matches)
                     :positions phrase-matches})
             results)))
       {}
       (group-by (juxt :doc-id :field)
                 (mapcat (fn [{:keys [word doc-matches]}]
                           (mapcat (fn [[doc-id field-data]]
                                     (map (fn [[field positions]]
                                            {:word word
                                             :doc-id doc-id
                                             :field field
                                             :positions (:position positions)})
                                          field-data))
                                   doc-matches))
                         word-positions))))))

(defn calculate-phrase-score
  "Calculate bonus score for phrase matches"
  [phrase-matches total-words]
  (if phrase-matches
    (let [phrase-count (:phrase-matches phrase-matches)
          proximity-bonus (/ phrase-count total-words)
          order-bonus (if (> phrase-count 0) 0.5 0)]
      (+ proximity-bonus order-bonus))
    0))

(defn search
  ([query] (search query *index*))
  ([query {{avg-fields ::wc-avg :as documents} :document :as index}]
   (letfn [(find-word-index [word]
             (get-in index [:word word]))
           (rank-word [word]
             (letfn [(rank-index [word-index]
                       (let [{:keys [df]} word-index
                             process-documents (dissoc word-index :df)
                             idf (Math/log (+ 1 (/ (- (::count documents 0) df) (+ df 0.5))))
                             results (reduce-kv
                                      (fn [ratings id {fields ::index}]
                                        (reduce-kv
                                         (fn [ratings field {:keys [tf]}]
                                           (let [doc-length (get-in documents [id ::index field :wc] 0)
                                                 doc-avg (get avg-fields field)
                                                 score (b25-score tf idf doc-length doc-avg)]
                                             (assoc ratings [id field] score)))
                                         ratings
                                         fields))
                                      {}
                                      process-documents)]
                         {:scores results :word-data word-index}))]
               (let [exact (find-word-index (*stem* word))]
                 (cond
                   (nil? word) nil
                   exact (rank-index exact)
                   :else
                   (let [stemmed (*stem* word)
                         similar-words (keep
                                        (fn [_word]
                                          (let [distance (levenshtein stemmed _word)]
                                            (when (< distance 3)
                                              _word)))
                                        (keys (:word index)))]
                     (when (seq similar-words)
                       (let [combined-results (reduce-kv
                                               (fn [result _ word-index]
                                                 (let [{:keys [scores]} (rank-index word-index)]
                                                   (merge-with + result scores)))
                                               nil
                                               (select-keys (:word index) similar-words))]
                         {:scores combined-results :word-data nil})))))))
           (rank-words [words]
             (keep
              (fn [word]
                (when-let [ranking (rank-word word)]
                  {:word word
                   :ranking (:scores ranking)
                   :word-data (:word-data ranking)}))
              words))
           (apply-phrase-matching [ranking query-words]
             (if (> (count query-words) 1)
               (let [word-positions (keep (fn [{:keys [word word-data]}]
                                            (when word-data
                                              {:word word :doc-matches (dissoc word-data :df)}))
                                          ranking)
                     phrase-matches (find-phrase-matches word-positions 3)]

                 (map (fn [{:keys [ranking] :as data}]
                        (assoc data :ranking
                               (reduce-kv
                                (fn [scores doc-field-key score]
                                  (let [phrase-bonus (calculate-phrase-score
                                                      (get phrase-matches doc-field-key)
                                                      (count query-words))
                                        boosted-score (* score (+ 1 phrase-bonus))]
                                    (assoc scores doc-field-key boosted-score)))
                                ranking
                                ranking)))
                      ranking))
               ranking))
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
           (find-words [ranking doc]
             (reduce
              (fn [r {:keys [word ranking]}]
                (if (contains? ranking doc)
                  (conj r word)
                  r))
              #{}
              ranking))
           (sum-rankings
             [ranking]
             (when ranking
               (reduce-kv
                (fn [r k v]
                  (assoc r (conj k (find-words ranking k)) v))
                nil
                (apply merge-with + (map :ranking ranking)))))
           (sort-results
             [result]
             (when result (sort-by val > result)))
           (return-docs
             [results]
             (distinct
              (map
               (fn [[[_ref _ words]]]
                 (assoc (get-in index [:document _ref])
                   :words words))
               results)))]
     (let [query-words (tokenize query)]
       (-> query-words
           rank-words
           (apply-phrase-matching query-words)
           apply-weights
           sum-rankings
           sort-results
           return-docs)))))

(defn calculate-proximity-score
  "Calculate how close words are to each other in a document field"
  [query-words doc field word-results]
  (let [word-positions (keep (fn [{:keys [word word-data]}]
                               (when-let [positions (get-in word-data [doc ::index field :position])]
                                 [word positions]))
                             word-results)]
    (if (< (count word-positions) 2)
      0.0 ; No proximity bonus for single words
      (let [all-positions (mapcat second word-positions)
            min-pos (apply min all-positions)
            max-pos (apply max all-positions)
            span (- max-pos min-pos)
            word-count (count query-words)]
        ;; Better proximity = lower span relative to word count
        ;; Score from 0.0 (far apart) to 1.0 (adjacent)
        (max 0.0 (min 1.0 (/ (- 10 span) 10.0)))))))

(defn calculate-phrase-bonus
  "Calculate bonus for having multiple query words in same document/field"
  [query-words doc field word-results]
  (let [words-found (count (filter (fn [{:keys [word-data]}]
                                     (get-in word-data [doc ::index field]))
                                   word-results))
        total-words (count query-words)]
    (if (< words-found 2)
      0.0
      ;; Linear bonus: 20% per additional word beyond first
      (* 0.2 (- words-found 1)))))

(defn calculate-proximity-bonus
  "Calculate bonus based on how close words are to each other"
  [query-words doc field word-results]
  (let [word-positions (keep (fn [{:keys [word word-data]}]
                               (when-let [positions (get-in word-data [doc ::index field :position])]
                                 [word positions]))
                             word-results)]
    (if (< (count word-positions) 2)
      0.0
      (let [all-positions (mapcat second word-positions)
            min-pos (apply min all-positions)
            max-pos (apply max all-positions)
            span (- max-pos min-pos)
            ;; Better proximity = higher bonus (max 30%)
            proximity-score (max 0.0 (min 1.0 (/ (- 15 span) 15.0)))]
        (* 0.3 proximity-score)))))

(defn calculate-coverage-bonus
  "Calculate bonus for query word coverage (what % of query words are found)"
  [query-words doc field word-results]
  (let [words-found (count (filter (fn [{:keys [word-data]}]
                                     (get-in word-data [doc ::index field]))
                                   word-results))
        total-words (count query-words)
        coverage-ratio (/ words-found total-words)]
    ;; Bonus for high coverage: 25% bonus for 100% coverage
    (if (> coverage-ratio 0.5) ; Only bonus if >50% words found
      (* 0.25 (- coverage-ratio 0.5) 2) ; Scale from 50%-100% to 0%-25%
      0.0)))

(defn search-enhanced
  "Comprehensive search with BM25, phrase matching, and proximity scoring"
  ([query] (search-enhanced query *index*))
  ([query {{avg-fields ::wc-avg :as documents} :document :as index}]
   (when (and query index)
     (let [query-words (tokenize query)
           single-word? (= 1 (count query-words))

           ;; Get base BM25 scores for each word
           word-results (keep (fn [word]
                                (when-let [word-data (get-in index [:word (*stem* word)])]
                                  (let [{:keys [df]} word-data
                                        process-documents (dissoc word-data :df)
                                        idf (Math/log (+ 1 (/ (- (::count documents 0) df) (+ df 0.5))))
                                        scores (reduce-kv
                                                (fn [ratings id {fields ::index}]
                                                  (reduce-kv
                                                   (fn [ratings field {:keys [tf]}]
                                                     (let [doc-length (get-in documents [id ::index field :wc] 0)
                                                           doc-avg (get avg-fields field)
                                                           score (b25-score tf idf doc-length doc-avg)]
                                                       (assoc ratings [id field] score)))
                                                   ratings
                                                   fields))
                                                {}
                                                process-documents)]
                                    {:word word :scores scores :word-data word-data})))
                              query-words)

           ;; Combine base scores
           base-scores (apply merge-with + (map :scores word-results))

           ;; Apply enhancements based on query type
           enhanced-scores (if single-word?
                            ;; Single word: just use base BM25 scores
                             base-scores
                            ;; Multi-word: apply phrase and proximity bonuses
                             (reduce-kv
                              (fn [scores [doc field :as key] base-score]
                                (let [;; Calculate different bonus types
                                      phrase-bonus (calculate-phrase-bonus query-words doc field word-results)
                                      proximity-bonus (calculate-proximity-bonus query-words doc field word-results)
                                      coverage-bonus (calculate-coverage-bonus query-words doc field word-results)

                                     ;; Combine bonuses with diminishing returns
                                      total-multiplier (+ 1.0
                                                          phrase-bonus
                                                          proximity-bonus
                                                          coverage-bonus)

                                      final-score (* base-score total-multiplier)]
                                  (assoc scores key final-score)))
                              {}
                              base-scores))

           ;; Apply field weights
           weighted-scores (if *weights*
                             (reduce-kv
                              (fn [scores [doc field :as key] score]
                                (if-let [weight (get *weights* field)]
                                  (assoc scores key (* score weight))
                                  scores))
                              {}
                              enhanced-scores)
                             enhanced-scores)

           ;; Sort and return documents
           sorted-results (sort-by val > weighted-scores)
           doc-refs (distinct (map (comp first first) sorted-results))]

       (map #(get-in index [:document %]) doc-refs)))))

(defn generate-snippet
  "Generate a snippet with character-based limits and highlighted matches"
  [content query-words & {:keys [max-chars highlight-tag]
                          :or {max-chars 100
                               highlight-tag ["**" "**"]}}]
  (let [content-lower (clojure.string/lower-case content)
        query-lower (map clojure.string/lower-case query-words)
        [start-tag end-tag] highlight-tag

        ;; Find all match positions (character-based)
        match-ranges (mapcat (fn [word]
                               (let [word-lower (clojure.string/lower-case word)]
                                 (loop [pos 0 matches []]
                                   (let [found (.indexOf content-lower word-lower pos)]
                                     (if (>= found 0)
                                       (recur (+ found (count word-lower))
                                              (conj matches {:start found
                                                             :end (+ found (count word-lower))
                                                             :word word}))
                                       matches)))))
                             query-words)

        ;; Find the best snippet window around matches
        snippet-info (if (empty? match-ranges)
                      ;; No matches found, take from beginning
                       {:start 0
                        :end (min max-chars (count content))
                        :matches []}
                      ;; Find optimal window around matches
                       (let [first-match (apply min (map :start match-ranges))
                             last-match (apply max (map :end match-ranges))
                             match-span (- last-match first-match)

                            ;; If matches fit in max-chars, center them
                            ;; Otherwise, start from first match
                             snippet-start (if (<= match-span max-chars)
                                             (max 0 (- first-match
                                                       (/ (- max-chars match-span) 2)))
                                             first-match)
                             snippet-end (min (count content)
                                              (+ snippet-start max-chars))

                            ;; Adjust start if we're at the end
                             final-start (max 0 (- snippet-end max-chars))
                             final-end (min (count content) (+ final-start max-chars))]

                         {:start (int final-start)
                          :end (int final-end)
                          :matches (filter #(and (>= (:start %) final-start)
                                                 (< (:end %) final-end))
                                           match-ranges)}))

        ;; Extract snippet text
        snippet-text (subs content (:start snippet-info) (:end snippet-info))

        ;; Apply highlighting
        highlighted-text (reduce (fn [text match]
                                   (let [relative-start (- (:start match) (:start snippet-info))
                                         relative-end (- (:end match) (:start snippet-info))
                                         before (subs text 0 relative-start)
                                         matched (subs text relative-start relative-end)
                                         after (subs text relative-end)]
                                     (str before start-tag matched end-tag after)))
                                 snippet-text
                                ;; Sort matches by position (right to left for string replacement)
                                 (reverse (sort-by :start (:matches snippet-info))))]

    {:snippet highlighted-text
     :start-char (:start snippet-info)
     :end-char (:end snippet-info)
     :total-chars (count content)
     :match-count (count (:matches snippet-info))
     :has-more-before (> (:start snippet-info) 0)
     :has-more-after (< (:end snippet-info) (count content))}))

(defn extract-match-positions
  "Extract positions where query words match in content"
  [content query-words]
  (let [content-words (tokenize content)
        stemmed-query (map *stem* query-words)]
    (keep-indexed
     (fn [idx word]
       (when (some #(= (*stem* word) %) stemmed-query)
         idx))
     content-words)))

(defn search-with-snippets
  "Enhanced search that returns results with snippets using character-based limits"
  ([query] (search-with-snippets query *index*))
  ([query index]
   (let [base-results (search query index)
         query-words (reduce into #{} (map :words base-results))]
     (map (fn [{{:keys [content title]} ::raw :as doc}]
            ; (println "TITLE: " title)
            ; (println "CONTENT:\n" content)
            (let [doc-ref (or (:ref doc) (:id doc))
                  content (or content
                              title
                              "")
                  snippet-data (when (and (string? content) (seq query-words))
                                 (generate-snippet content query-words))]
              (assoc doc
                :snippet (:snippet snippet-data)
                :snippet-meta (dissoc snippet-data :snippet))))
          base-results))))

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
           _value (toddler/use-delayed value 100)
           [results set-results!] (hooks/use-state nil)]
       (hooks/use-effect
         [_value]
         (if (empty? _value)
           (set-results! nil)
           (set-results! (search-with-snippets _value index))))
       results)))

(comment
  (def query "hook")
  (def query "positive notification")
  (*stem* "notify")
  (*stem* "notification")
  (*stem* "notifications")
  (def index
    (build-index
     (toddler.search.docs/prepare-index
      (toddler.search.docs/make-config ""))))
  (binding [*weights* {:content 0.7 :title 1.8}]
    (time (search "toddler route" index)))
  (get-in index [:word "hook"])
  (-> index :document)
  (def index nil)
  (build-index *1)
  (alter-var-root
   #'*index*
   (fn [_] index))
  (keys (:word index))
  (-> config :mds prepare-index)
  (search "calendar")
  (time (search "caluddar field" index))
  (time (search-with-snippets "calendar mode integer" index))
  (time (search "calendar mode integer" index))
  (contains? *stop-words* "")
  (count (keys index))
  (-> index :document :count)
  (-> index :word (get "hook")))
