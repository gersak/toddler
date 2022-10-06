(ns toddler.ui)

(declare __commponents__)

(defmacro defcomponent [_name key]
  `(helix.core/defnc ~_name [props# ref#]
     {:wrap [(toddler.ui/forward-ref)]}
     ; (println "GETTING KEY: " ~key)
     ; (println "DODODMPONENTS: " helix.hooks/use-context toddler.ui/__components__)
     ; (println "PRDUC")
     (let [components# (helix.hooks/use-context __components__)
           component# (get components# ~key)
           children# (helix.children/children props#)]
       (when component#
         (helix.core/$ component# {:ref ref# :& props#} children#)))))


; (defmacro defcomponent [_name]
;   (let [context (symbol (str "$" _name))]
;     `(do
;        (def ^js ~context (helix.core/create-context))
;        (helix.core/defnc ~_name [props#]
;          (let [component# (helix.hooks/use-context ~context)
;                children# (helix.children/children props#)]
;            (helix.core/$ component# {:& props#} children#))))))
