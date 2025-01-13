(ns toddler.core)

;; TODO - write macro for lazy component loading using Suspense
; (defmacro lazy-component [module component]
;   `(helix.core/defnc
;      ~'react/lazy
;      (fn []
;        (->
;         (shadow.loader/load ~module)
;         (.then (fn [_] #js {:default ~'~component}))))))
