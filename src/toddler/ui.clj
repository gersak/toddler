(ns toddler.ui)


(defmacro defcomponent [_name]
  (let [context (symbol (str "$" _name))]
    `(do
       (def ^js ~context (helix.core/create-context))
       (helix.core/defnc ~_name [props#]
         (let [component# (helix.hooks/use-context ~context)
               children# (helix.children/children props#)]
           (helix.core/$ component# {:& props#} children#))))))
