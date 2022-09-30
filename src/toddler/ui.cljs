(ns toddler.ui
  (:require-macros [toddler.ui :refer [defcomponent]])
  (:require
    [helix.core :refer [create-context defnc]]
    [helix.hooks :as hooks]))


(defcomponent text-field)
(defcomponent integer-field)
(defcomponent float-field)
(defcomponent input-field)
(defcomponent dropdown-field)
(defcomponent multiselect-field)
(defcomponent timestamp-field)
(defcomponent period-field)


; (def ^:dynamic ^js *checkbox* (create-context))
; (def ^:dynamic ^js *button* (create-context))
; (def ^:dynamic ^js *popup* (create-context))
; (def ^:dynamic ^js *dropdown-field* (create-context))
; (def ^:dynamic ^js *multiselect-field* (create-context))
; (def ^:dynamic ^js *text-field* (create-context))
; (def ^:dynamic ^js *row* (create-context))
; (def ^:dynamic ^js *column* (create-context))
; (def ^:dynamic ^js *modal* (create-context))
; (def ^:dynamic ^js *checkbox-field* (create-context))
; (def ^:dynamic ^js *integer-field* (create-context))
; (def ^:dynamic ^js *float-field* (create-context))
; (def ^:dynamic ^js *text-field* (create-context))
; (def ^:dynamic ^js *dropdown-field* (create-context))
; (def ^:dynamic ^js *multiselect-field* (create-context))
; (def ^:dynamic ^js *timestamp-field* (create-context))
; (def ^:dynamic ^js *timeperiod-field* (create-context))



; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))

; (defnc integer-field [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
; (defnc checkbox [props] ($ (hooks/use-context *checkbox*)))
