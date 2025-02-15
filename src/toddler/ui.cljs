(ns toddler.ui
  (:refer-clojure :exclude [identity])
  (:require-macros [toddler.ui :refer [defcomponent !]])
  (:require
   ["react" :as react]
   [helix.core
    :refer [create-context provider
            defnc $ fnc defhook]]
   [helix.children :as c]
   [helix.hooks :as hooks]))

(declare avatar row column form checkbox drawer tab tabs grid
         button buttons simplebar popup popup-area popup-element option input
         field dropdown img tag header identity tooltip
         textarea action checklist card card-action
         card-actions calendar-year-dropdown calendar-day calendar-week calendar-month calendar-time
         calendar calendar-period search-field identity-field identity-multiselect-field text-field integer-field
         float-field currency-field input-field dropdown-field multiselect-field timestamp-field
         timestamp-period-field date-field date-period-field boolean-field checklist-field password-field
         idle-field copy-field
         table table-cell table-row table-header-row enum-header currency-header boolean-header text-header
         user-header timestamp-header plain-header boolean-cell integer-cell float-cell
         text-cell enum-cell currency-cell hash-cell uuid-cell identity-cell
         timestamp-cell expand-cell delete-cell forward-ref modal-background modal-dialog)

(def ^js __components__ (create-context))

(def forward-ref ^js react/forwardRef)

(defcomponent avatar :avatar)
(defcomponent row :row)
(defcomponent column :column)
(defcomponent form :form)
(defcomponent checkbox :checkbox)
(defcomponent button :button)
(defcomponent buttons :buttons)
(defcomponent simplebar :simplebar)
(defcomponent popup-area :popup-area)
(defcomponent popup-element :popup-element)
; (defcomponent option :option)
(defcomponent dropdown :dropdown)
(defcomponent img :img)
(defcomponent tag :tag)
(defcomponent header :header)
(defcomponent identity :identity)
(defcomponent tooltip :tooltip)
(defcomponent field :field)
(defcomponent input :input)
(defcomponent textarea :textarea)
(defcomponent action :action)

(defcomponent modal-background :modal/background)
(defcomponent modal-dialog :modal/dialog)

(defcomponent card :card)

(defcomponent tabs :tabs)
(defcomponent tab :tab)
(defcomponent grid :grid)

(defcomponent calendar-day :calendar/day)
(defcomponent calendar-month :calendar/month)
(defcomponent calendar :calendar)
(defcomponent calendar-period :calendar/period)

;; Fields
(defcomponent search-field :field/search)
(defcomponent identity-field :field/identity)
(defcomponent identity-multiselect-field :field/identity-multiselect)
(defcomponent text-field :field/text)
(defcomponent integer-field :field/integer)
(defcomponent float-field :field/float)
(defcomponent currency-field :field/currency)
(defcomponent input-field :field/input)
(defcomponent password-field :field/password)
(defcomponent dropdown-field :field/dropdown)
(defcomponent multiselect-field :field/multiselect)
(defcomponent timestamp-field :field/timestamp)
(defcomponent timestamp-period-field :field/timestamp-period)
(defcomponent date-field :field/date)
(defcomponent date-period-field :field/date-period)
(defcomponent boolean-field :field/boolean)
(defcomponent checklist-field :field/checklist)
(defcomponent idle-field :field/idle)
(defcomponent copy-field :field/copy)

;; Table
(defcomponent table :table)
(defcomponent table-row :table/row)
(defcomponent table-cell :table/cell)
(defcomponent table-header-row :table/header-row)

(defcomponent enum-header :header/enum)
(defcomponent currency-header :header/currency)
(defcomponent boolean-header :header/boolean)
(defcomponent text-header :header/text)
(defcomponent user-header :header/user)
(defcomponent timestamp-header :header/timestamp)
(defcomponent plain-header :header/plain)

(defcomponent boolean-cell :cell/boolean)
(defcomponent integer-cell :cell/integer)
(defcomponent float-cell :cell/float)
(defcomponent text-cell :cell/text)
(defcomponent enum-cell :cell/enum)
(defcomponent currency-cell :cell/currency)
(defcomponent hash-cell :cell/hashed)
(defcomponent uuid-cell :cell/uuid)
(defcomponent identity-cell :cell/identity)
(defcomponent timestamp-cell :cell/timestamp)
(defcomponent expand-cell :cell/expand)
(defcomponent delete-cell :cell/delete)

(defhook use-component [id]
  (get (hooks/use-context __components__) id))

(defnc UI
  "Provider of UI components. It will provide toddler.ui/__components__
  context that is expected to be map of keywords maped to component
  implementation.
  
  For list of target components check out toddler.ui namespace"
  [{:keys [components] :as props}]
  (provider
   {:context __components__
    :value components}
   (c/children props)))

(defn wrap-ui
  "Wrapper for UI component. UI is Toddler ui components
  provider. Provider expects map with bound components to
  matching keys in toddler.ui namespace"
  ([component components]
   (fnc UI [props]
     ($ UI {:components components}
        ($ component {:& props})))))

(defnc ExtendUI
  "Component that is extending toddler.ui/__commponents__ context
  by merging components in props to existing value.

  New components will be available to children components"
  [{:keys [components] :as props}]
  (let [current (hooks/use-context __components__)]
    (provider
     {:context __components__
      :value (merge current components)}
     (c/children props))))

(defn extend-ui
  "Wrapper that will extend Toddler UI by merging new
  components onto currently available components."
  ([component components]
   (fnc ExtendUI [props]
     ($ ExtendUI {:components components}
        ($ component {:& props})))))
