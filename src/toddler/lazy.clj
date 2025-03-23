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
                 {:wrap [(toddler.ui/forward-ref)]}
                 [props# _ref#]
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
                   (when component#
                     (let [props# (if _ref#
                                    (assoc props# :ref _ref#)
                                    props#)]
                       (helix.core/$ component# {& props#})))))))))))

(comment
  (macroexpand-1
   '(load-components
     "markdown"
     ::show toddler.md/show
     ::img toddler.md/img)))
