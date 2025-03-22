(ns toddler.lazy)

#_(defmacro load-components
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

#_(defmacro load-components
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
    ([& bindings]
     (let [bindings (apply hash-map bindings)
           sym-mapping (reduce
                        (fn [r k] (assoc r k (gensym "lazy_component")))
                        nil
                        (keys bindings))]
       `(do
          ~@(for [[k target] bindings
                  :let [lazy-sym (get sym-mapping k)]]
              `(do
                 (def ~lazy-sym (shadow.lazy/loadable ~target))
                 (helix.core/defnc ~(symbol (name k))
                   [props#]
                   (let [component# (toddler.lazy/use-lazy ~k)]
                     (helix.hooks/use-effect
                       :once
                       (when (= component# toddler.lazy/not-found)
                         (shadow.cljs.modern/js-await
                          [_target# (shadow.lazy/load ~lazy-sym)]
                          (.log js/console "LOADED: " _target#)
                          (swap! toddler.lazy/tank assoc ~k _target#))))
                     (.log js/console "RENDERING: " component#)
                     #_(helix.core/$ component# {& props#})))))))))

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
  ([& bindings]
   (let [bindings (apply hash-map bindings)
         sym-mapping (reduce
                      (fn [r k] (assoc r k (gensym "lazy_component")))
                      nil
                      (keys bindings))]
     `(do
        ~@(for [[k target] bindings
                :let [lazy-sym (get sym-mapping k)]]
            `(do
               (def ~lazy-sym (shadow.lazy/loadable ~target))
               (helix.core/defnc ~(symbol (name k))
                 [props#]
                 (let [[loaded# load#] (helix.hooks/use-state (true? (get (deref ~'toddler.lazy/tank) ~k false)))
                       component# (helix.hooks/use-memo
                                    [loaded#]
                                    (when loaded#
                                      (get (deref ~'toddler.lazy/tank) ~k)))]
                   (helix.hooks/use-effect
                     :once
                     (when-not component#
                       (shadow.cljs.modern/js-await
                        [_target# (shadow.lazy/load ~lazy-sym)]
                        (swap! toddler.lazy/tank assoc ~k _target#)
                        (load# true))))
                   (when component# (helix.core/$ component# {& props#}))))))))))

(comment
  (macroexpand-1
   '(load-components
     "markdown"
     ::show toddler.md/show
     ::img toddler.md/img)))
