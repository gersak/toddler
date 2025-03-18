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
        (toddler.lazy/load* ~_name #(hash-map ~@bindings))
        ~@(for [k ks]
            `(helix.core/defnc ~(symbol (name k))
               [props#]
               (let [component# (toddler.lazy/use-lazy ~k)]
                 (helix.core/$ component# {& props#}))))))))

; (defmacro load
;   "Macro for lazy loading modules and exposing components from
;   loaded module. First argument is name of module, that is what
;   you named module in shadow-cljs.edn.
;
;   Rest of arguments should be pairs of keywords that map to
;   components inside of target module. I.E.
;
;   ```clojure
;   (lazy/load
;    \"chartjs\"
;    ::Chart toddler.chart-js/Chart
;    ::John  toddler.dummy/john
; o
;    ::Doe   toddler.dummy/doe)
;   ```"
;   ([_name {:keys [components hooks]}]
;    (let [component-ks (take-nth 2 components)
;          hook-ks (take-nth 2 hooks)]
;      `(do
;         (toddler.lazy/load* ~_name #(hash-map ~@components))
;         ~@(for [k component-ks]
;             `(helix.core/defnc ~(symbol (name k))
;                [props#]
;                (let [component# (toddler.lazy/use-lazy ~k)]
;                  (helix.core/$ component# {& props#}))))
;         ~@(for [k hook-ks]
;             `(helix.core/defhook ~(symbol (name k))
;                [props#]
;                (let [component# (toddler.lazy/use-lazy ~k)]
;                  (helix.core/$ component# {& props#}))))))))
