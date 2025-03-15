(ns toddler.lazy)

(defmacro load
  "Macro for lazy loading modules and exposing components from
  loaded module. First argument is name of module, that is what
  you named module in shadow-cljs.edn.
  
  Rest of arguments should be pairs of keywords that map to
  components inside of target module. I.E.
  
  ```clojure
  (lazy/load
   \"chartjs\"
   ::Chart toddler.chart-js/Chart
   ::John  toddler.dummy/john
   ::Doe   toddler.dummy/doe)
  ```"
  ([_name & bindings]
   (let [ks (take-nth 2 bindings)]
     `(do
        (toddler.lazy/load* ~_name #(hash-map ~@bindings))
        ~@(for [k ks]
            `(helix.core/defnc ~(symbol (name k))
               [props#]
               (let [component# (toddler.lazy/use-component ~k)]
                 (helix.core/$ component# {& props#}))))))))
