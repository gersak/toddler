(ns toddler.lazy)

(defmacro load-components
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
o
   ::Doe   toddler.dummy/doe)
  ```"
  ([_name & bindings]
   (let [ks (take-nth 2 bindings)]
     `(do
        ~@(for [k ks]
            `(helix.core/defnc ~(symbol (name k))
               [props#]
               (let [component# (toddler.lazy/use-lazy ~k)]
                 (helix.hooks/use-effect
                   :once
                   (toddler.lazy/load* ~_name #(hash-map ~@bindings)))
                 (helix.core/$ component# {& props#}))))))))
