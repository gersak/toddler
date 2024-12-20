(ns toddler.graphql
  (:require
   clojure.set
   clojure.string
   #?(:cljs goog.string.format)))

(def ^:dynamic *variable-bindings* nil)
(def ^:dynamic *with-indent* true)
(def ^:dynamic *indent* "  ")
(def ^:dynamic *level* 0)

(defprotocol GraphQLTransformProtocol
  (->graphql [this] [this params])
  (<-graphql [this]))

(def f #?(:clj format
          :cljs goog.string.format))

(def j clojure.string/join)

(def generate-indent
  (memoize
   (fn [level]
     (j (repeat level *indent*)))))

(defn indent-lines [level lines]
  (str
   (generate-indent level)
   (j (str \newline (generate-indent level)) lines)))

(defn indent [level text]
  (indent-lines level (clojure.string/split-lines text)))

(extend-type nil
  GraphQLTransformProtocol
  (->graphql [_] ""))

(defn- map->graphql
  [this]
  (str
   \{
   (j " "
      (reduce-kv
       (fn [r k v]
         (conj r (str (name k) \: (->graphql v))))
       []
       this))
   \}))

(defn- seq->graphql
  [this]
  (str \[ (j " " (mapv ->graphql this)) \]))

(extend-protocol GraphQLTransformProtocol
  #?(:clj String :cljs string)
  (->graphql [this] (pr-str this))
  #?(:clj java.lang.Number :cljs number)
  (->graphql [this] (str this))
  #?@(:clj [java.lang.Integer (->graphql [this] (str this))])
  #?@(:clj [java.lang.Float (->graphql [this] (str this))])
  #?@(:clj [java.lang.Double (->graphql [this] (str this))])
  #?@(:cljs [PersistentArrayMap (->graphql [this] (map->graphql this))]
      :bb [clojure.lang.PersistentArrayMap (->graphql [this] (map->graphql this))])
  #?(:clj clojure.lang.APersistentMap :cljs PersistentHashMap :bb clojure.lang.PersistentHashMap)
  (->graphql [this] (map->graphql this))
  #?(:bb clojure.lang.PersistentVector :clj clojure.lang.APersistentVector :cljs PersistentVector)
  (->graphql [this] (seq->graphql this))
  #?(:bb clojure.lang.PersistentHashSet :clj clojure.lang.APersistentSet :cljs PersistentHashSet)
  (->graphql [this] (seq->graphql this))
  #?(:clj clojure.lang.LazySeq :cljs LazySeq)
  (->graphql [this] (seq->graphql this))
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (->graphql [this] (clojure.string/replace (name this) #"-|\s" "_"))
  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (->graphql [this] (name this))
  #?(:clj java.util.UUID :cljs cljs.core/UUID)
  (->graphql [this] (->graphql (str this)))
  #?@(:cljs [js/Date (->graphql [this] (.stringify js/JSON this))])
  #?@(:clj [java.util.Date (->graphql [this] (.. this toInstant toString))])
  #?(:clj java.lang.Boolean :cljs boolean)
  (->graphql [this] (str this))
  nil
  (->graphql [_] "null"))

(defn args->graphql
  [m]
  (when (not-empty m)
    (let [r (->graphql m)]
      (subs r 1 (dec (count r))))))

(defrecord GraphQLSelection [selection]
  GraphQLTransformProtocol
  (->graphql
    [_]
    {:pre [(map? selection)
           (not-empty selection)]}
    (let [lines (reduce-kv
                  ;; Go through selections
                 (fn [r k v]
                    ;; And for each selection conj
                      ;; if value of selection is nil than
                      ;; return original field
                   (if (nil? v)
                              ;; Otherwise map function to every alias/selections 
                     (conj r (name k))
                     (concat
                      r
                      (cond->>
                       (map
                        (fn [{:keys [selections alias args]}]
                          (when (nil? k)
                            (throw
                             (ex-info
                              "Can't apply selection with key 'nil'"
                              {:key k
                               :value v
                               :selections selection})))
                          (str
                                  ;; if there is alias than rename original field
                           (if alias
                             (str (clojure.core/name alias) \: (name k))
                             (name k))
                                  ;; If arguments are present, than cat args to gragphql
                           (when (not-empty args) (str \( (args->graphql args) \)))
                                  ;; and finally if selections are not empty
                                  ;; recur with indent
                           (when (not-empty selections)
                             (if *with-indent*
                               (f " {\n%s\n%s}"
                                  (binding [*level* (inc *level*)]
                                    (->graphql (GraphQLSelection. selections)))
                                  (generate-indent *level*))
                               (f "{%s}" (->graphql (GraphQLSelection. selections)))))))
                        v)))))
                 []
                 selection)]
      (if *with-indent*
        (indent-lines *level* lines)
        (clojure.string/join " " lines)))))

(defrecord GraphQLQuery [name alias selection args]
  GraphQLTransformProtocol
  (->graphql
    [_]
    (assert (some? name) "Query name not provided")
    ; (assert (map? selection) "Cannot create query without selection")
    ; (assert (not-empty selection) "Cannot create query without selection")
    (let [name' (when name
                  (if alias
                    (str (clojure.core/name alias) \: (clojure.core/name name))
                    (clojure.core/name name)))
          args' (if (not-empty args)
                  (str \( (args->graphql args) \))
                  "")]
      (if *with-indent*
        (str
         (generate-indent (inc *level*)) name'
         args'
         (when (not-empty selection)
           (str
            \space \{ \newline
            (binding [*level* (+ *level* 2)]
              (->graphql (->GraphQLSelection selection)))
            (str \newline (generate-indent (inc *level*)) \} \newline))))
        (f "%s %s{%s}"
           name'
           args'
           (when (not-empty selection) (->graphql (->GraphQLSelection selection))))))))

(defrecord GraphQLMutation [name alias args selection]
  GraphQLTransformProtocol
  (->graphql
    [_]
    (assert (some? name) "Mutation name not provided")
    ; (assert (not-empty args) "Cannot mutate without arguments")
    (let [name' (when name
                  (if alias
                    (str (clojure.core/name alias) \: (clojure.core/name name))
                    (clojure.core/name name)))
          args' (if args
                  (str \( (args->graphql args) \))
                  "")]
      (if *with-indent*
        (str (generate-indent (inc *level*)) name' args'
             (when (not-empty selection)
               (str \space \{ \newline
                    (binding [*level* (+ *level* 2)]
                      (->graphql (->GraphQLSelection selection)))
                    \newline (generate-indent (inc *level*)) \} \newline)))
        (f "mutation {%s%s{%s}}"
           name'
           args'
           (->graphql (->GraphQLSelection selection)))))))

(defrecord GraphQLSubscription [name selection args]
  GraphQLTransformProtocol
  (->graphql
    [_]
    {:pre [(some? name)]}
    (let [args' (when args (str \( (args->graphql args) \)))
          selection' (->graphql (->GraphQLSelection selection))]
      (if *with-indent*
        (indent
         *level*
         (if (not-empty selection)
           (str "subscription {\n"
                (indent (inc *level*) (str (clojure.core/name name) args' \space \{)) \newline
                (indent
                 (+ 2 *level*)
                 (binding [*level* (inc *level*)]
                   selection'))
                \newline
                (indent (inc *level*) (str \})) \newline
                \})
           (str "subscription {\n"
                (indent (inc *level*) (str (clojure.core/name name) args'))
                \newline \})))
        (f "subscription {%s%s%s}"
           (clojure.core/name name)
           (or args' "")
           (if (not-empty selection) (str \{ selection' \}) ""))))))

(defrecord GraphQLPayload [query operation variables])

(defn wrap-queries [& queries]
  (str \{ \newline (clojure.string/join "" queries) \}))

(defn wrap-mutations [& mutations]
  (if (not-empty *variable-bindings*)
    (letfn [(print-binding [[a b]]
              (str "$" (name a) ":" (if (vector? b) [(symbol (name (first b)))] (name b))))]
      (str "mutation("
           (clojure.string/join " " (map print-binding (partition 2 *variable-bindings*)))
           "){\n"
           (clojure.string/join "" mutations) \}))
    (str "mutation {\n" (clojure.string/join "" mutations) \})))

(defn gen-mutation
  ([{:keys [mutation variables types selection alias args]}]
   (let [variable-mapping (reduce-kv
                           (fn [r k _]
                             (assoc r k (gensym "var__")))
                           nil
                           variables)
         type-mapping (reduce-kv
                       (fn [r k v]
                         (if-let [t (get types k)]
                           (let [t' (name t)]
                             (conj r v
                                   (if-not (sequential? (get variables k)) t'
                                           (str \[ t' \]))))
                           (throw
                            (ex-info "Couldn't get variable type"
                                     {:variable v
                                      :types types}))))
                       []
                       variable-mapping)
         mutation (->graphql
                   (->GraphQLMutation
                    mutation alias
                    (reduce-kv
                     (fn [r k v]
                       (assoc r k (keyword (str \$ v))))
                     (merge variable-mapping args)
                     variable-mapping)
                    selection))]
     [mutation type-mapping (clojure.set/rename-keys variables variable-mapping)])))

(defn queries
  [queries]
  (apply
   wrap-queries
   (map
    (fn [{query-name :query
          selection :selection
          alias :alias
          args :args}]
      (let [query-name (name query-name)]
        (->graphql
         (->GraphQLQuery
          query-name alias selection args))))
    queries)))

(defn mutations
  [mutations]
  (as-> nil data
    (reduce
     (fn [[mutations declarations variables] mutation-map]
       (let [[mutation mutation-declarations mutation-variables] (gen-mutation mutation-map)]
         [(conj mutations mutation)
          (into declarations mutation-declarations)
          (merge variables mutation-variables)]))
     [[] [] nil]
     mutations)
    (let [[mutations declarations variables] data]
      (binding [*variable-bindings* declarations]
        {:query (apply
                 wrap-mutations
                 mutations)
         :variables variables}))))

(comment
  (gen-mutation
   {:mutation :syncUserList
    :variables {:user [{:name "jozo"}
                       {:name "tit"}
                       {:name "vrit"}]}
    :types {:user :UserInput}
    :selection {:name nil}})

  (let [{:keys [query variables]} (time
                                   (mutations
                                    [{:mutation :syncUserList
                                      :alias :users
                                      :variables {:user [{:name "jozo"}
                                                         {:name "tit"}
                                                         {:name "vrit"}]}
                                      :types {:user :UserInput}
                                      :selection {:name nil}}
                                     {:mutation :syncUserGroup
                                      :alias :groups
                                      :variables {:user_group {:name "TestGroup"}}
                                      :types {:user_group :UserGroupInput}
                                      :selection {:euuid nil
                                                  :name nil
                                                  :users [{:alias :dorks
                                                           :selections
                                                           {:euuid nil
                                                            :name nil}}]}}
                                     {:mutation "Toggle_something"
                                      :alias :active}]))]
    (def query query)
    (def variables variables)
    (println query)
    (clojure.pprint/pprint variables))

  (println
   (queries
    {:query :searchUser
     :alias "users"
     :selection {:euuid nil
                 :name nil
                 :groups [{:selection
                           {:euuid nil
                            :name nil}}]
                 :roles [{:selections
                          {:euuid nil
                           :name nil}}]}})))
