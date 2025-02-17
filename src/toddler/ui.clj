(ns toddler.ui)

(declare __commponents__)

(defmacro defcomponent
  "Wrapper macro around helix.core/defnc function that
  will try to pull key from `__components__` context and render
  found component."
  ([_name key]
   `(helix.core/defnc ~_name [props# ref#]
      {:wrap [(toddler.ui/forward-ref)]}
      (let [components# (helix.hooks/use-context __components__)
            component# (get components# ~key)
            children# (helix.children/children props#)]
        (when component#
          (helix.core/$ component# {:ref ref# :& props#} children#)))))
  ([_name key docs]
   `(helix.core/defnc ~_name
      ~docs
      [props# ref#]
      {:wrap [(toddler.ui/forward-ref)]}
      (let [components# (helix.hooks/use-context __components__)
            component# (get components# ~key)
            children# (helix.children/children props#)]
        (when component#
          (helix.core/$ component# {:ref ref# :& props#} children#))))))

(defmacro !
  "Macro that will try to pull key component from `__components__` context
  and render it with helix.core/$ macro
  
  I.E.  (! :button {:className \"positive\"} \"Good day\") 
  
  **IMPORTANT** - don't use this macro in conditionals like `when`,
  `if`,`cond` etc.
  
  This macro will use `helix.hooks/use-context` hook to
  get component from toddler UI. React won't like if it is after conditional
  because hook will sometimes be there and other time not.
  
  You will see what the problem is in browser console"
  ^{:style/indent 0
    :cljfmt/ident [:form]}
  [key & stuff]
  `(when-let [component# (get (helix.hooks/use-context __components__) ~key)]
     (helix.core/$ component# ~@stuff)))

(comment
  (macroexpand-1
   '($ :popup
       {:a 100} "Here he" " goes")))
