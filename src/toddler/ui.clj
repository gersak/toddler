(ns toddler.ui)

(declare __commponents__)

(defmacro defcomponent [_name key]
  `(helix.core/defnc ~_name [props# ref#]
     {:wrap [(toddler.ui/forward-ref)]}
     (let [components# (helix.hooks/use-context __components__)
           component# (get components# ~key)
           children# (helix.children/children props#)]
       (when component#
         (helix.core/$ component# {:ref ref# :& props#} children#)))))
