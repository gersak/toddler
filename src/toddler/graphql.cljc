(ns toddler.graphql
  #?(:cljs
     (:require-macros [toddler.graphql]))
  (:require
    clojure.set
    clojure.string
    #?(:cljs [cljs.core])
    #?(:cljs [clojure.edn])
    #?(:cljs goog.string.format)
    #?(:cljs com.cognitect.transit.types)
    [camel-snake-kebab.core :as csk]
    #?(:cljs [clojure.core.async :as async])
    #?(:cljs [cognitect.transit :as transit])))


(def ^:dynamic *variable-bindings*)
(def ^:dynamic *with-indent* true)
(def ^:dynamic *indent* "  ")
(def ^:dynamic *level* 0)


(defmacro with-variable-bindings
  [bindings & body]
  `(binding [*variable-bindings* ~bindings]
     ~@body))

(defprotocol GraphQLTransformProtocol
  (->graphql [this] [this params])
  (<-graphql [this]))


(def f #?(:clj format
          :cljs goog.string.format))


(def j clojure.string/join)


(defn clj-key [k] 
  (csk/->kebab-case-keyword k :separator #"[\s-_\&]+"))


(defn gql-key [k]
  (csk/->snake_case_keyword k :separator #"[\s-\&]+"))


(defn indent-rest 
  ([level] (when level (j (repeat level *indent*))))
  ([level line]
   (if level
     (str (indent-rest level) line)
     line)))

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


(defn scalar-arg [v]
  (cond 
    ;;
    (keyword? v) (str (name v))
    ;;
    (uuid? v) (str \" v \")  
    ;;
    :else (pr-str v)))

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
  #?@(:cljs [PersistentArrayMap (->graphql [this] (map->graphql this))])
  #?(:clj clojure.lang.APersistentMap :cljs PersistentHashMap)
  (->graphql [this] (map->graphql this))
  #?(:clj clojure.lang.APersistentVector :cljs PersistentVector)
  (->graphql [this] (seq->graphql this))
  #?(:clj clojure.lang.APersistentSet :cljs PersistentHashSet)
  (->graphql [this] (seq->graphql this))
  #?(:clj clojure.lang.LazySeq :cljs LazySeq)
  (->graphql [this] (seq->graphql this))
  #?(:clj clojure.lang.Keyword :cljs Keyword)
  (->graphql [this] (clojure.string/replace (name this) #"-|\s" "_"))
  #?(:clj clojure.lang.Symbol :cljs Symbol)
  (->graphql [this] (name this))
  #?@(:cljs [com.cognitect.transit.types/UUID (->graphql [this] (->graphql (str this)))])
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
                                       :selection selection})))
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
              (str "$" (name a) ":" (name b)))]
      (str "mutation(" 
           (clojure.string/join " " (map print-binding (partition 2 *variable-bindings*)))
           "){\n"
           (clojure.string/join "" mutations) \}))
    (str "mutation {\n" (clojure.string/join "" mutations) \})))


#?(:cljs
   (defn send-query
     ([query & 
       {:keys [on-error
               on-progress
               on-load
               operation-name 
               token
               variables
               url] :as data}]
      (let [result (async/chan)
            body #js {"operationName" operation-name 
                      "query" query
                      "variables" (clj->js variables)} 
            xhr (new js/XMLHttpRequest)]
        (.open xhr "POST" url)
        (.setRequestHeader xhr "Content-Type" "application/json")
        (.setRequestHeader xhr "Accept" "application/json")
        ;; This should be flaged by some env *variable*
        (when token
          (.setRequestHeader xhr "Authorization" (str "Bearer " token)))
        (.addEventListener xhr "error" 
                           (fn [evt] 
                             (.error js/console "Couldn't contact EYWA")
                             (let [body (.. evt -currentTarget -responseText)] 
                               (async/put!
                                 result
                                 (let [raw-data (.parse js/JSON body)
                                       data (js->clj raw-data :keywordize-keys true)]
                                   {:errors data}))
                               (when (ifn? on-error) (on-error evt)))))

        (when (ifn? on-progress) (.addEventListener xhr "progress" on-progress))
        (.addEventListener xhr "load" 
                           (fn [evt] 
                             (let [body (.. evt -currentTarget -responseText)] 
                               (async/put!
                                 result
                                 (case (.. evt -currentTarget -status)
                                   403 (ex-info
                                         "Not authorized"
                                         {:type :not-authorized})
                                   500 (ex-info
                                         "Server couldn't process GraphQL query"
                                         {:query query
                                          :data data
                                          :type :server-error})
                                   (let [raw-data (.parse js/JSON body)
                                         data (js->clj raw-data :keywordize-keys true)]
                                     data))))
                             (when (ifn? on-load) (on-load evt))))
        (.send xhr 
               (.stringify js/JSON body
                           (fn [_ v]
                             (cond-> v
                               (transit/uuid? v) str
                               (uuid? v) str))))
        result)))
   :clj
   (defn send-query 
     ([query] (send-query query nil))
     ([query variables] (send-query query variables nil))
     ([_ _ _]
      ;; TODO - reimplement this for JSON-RPC calls!!!!
      #_(let [graphql-schema @schema] 
          (lacinia/execute graphql-schema query variables context)))))
