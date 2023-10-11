(ns toddler.ui
  (:refer-clojure :exclude [identity])
  (:require-macros [toddler.ui :refer [defcomponent]])
  (:require
    ["react" :as react]
    [helix.core :refer [create-context]]))


(declare avatar row column form checkbox
         button simplebar popup option input clear
         field wrapper discard dropdown img header identity tooltip
         autosize-input idle-input action checklist card card-action
         card-actions calendar-year-dropdown calendar-day calendar-week calendar-month calendar-time
         calendar search-field identity-field identity-multiselect-field text-field integer-field
         float-field currency-field input-field dropdown-field multiselect-field timestamp-field
         timestamp-period-field date-field date-period-field boolean-field checklist-field table
         table-row table-header-row enum-header currency-header boolean-header text-header
         user-header timestamp-header plain-header boolean-cell integer-cell float-cell
         text-cell enum-cell currency-cell hash-cell uuid-cell identity-cell
         timestamp-cell expand-cell delete-cell forward-ref)




(def ^js __components__ (create-context))


(def forward-ref react/forwardRef)


(defcomponent avatar :avatar)
(defcomponent avatar-editor :avatar/editor)
(defcomponent row :row)
(defcomponent column :column)
(defcomponent form :form)
(defcomponent checkbox :checkbox)
(defcomponent button :button)
(defcomponent simplebar :simplebar)
(defcomponent popup :popup)
(defcomponent option :option)
(defcomponent input :input)
(defcomponent clear :clear)
(defcomponent close :close)
(defcomponent field :field)
(defcomponent wrapper :wrapper)
(defcomponent discard :discard)
(defcomponent dropdown :dropdown)
(defcomponent img :img)
(defcomponent header :header)
(defcomponent identity :identity)
(defcomponent tooltip :tooltip)
(defcomponent autosize-input :input/autosize)
(defcomponent action :action)
(defcomponent checklist :checklist)
(defcomponent drawer :drawer)


(defcomponent modal-avatar-editor :modal/avatar-editor)
(defcomponent modal-background :modal/background)
(defcomponent modal-dialog :modal/dialog)
(defcomponent modal-pavement :modal/pavement)
(defcomponent modal-strip :modal/strip)


(defcomponent card :card)
(defcomponent card-action :card/action)
(defcomponent card-actions :card/actions)


(defcomponent tabs :tabs)
(defcomponent tab :tab)


(defcomponent calendar-month-dropdown :calendar/month-dropdown)
(defcomponent calendar-year-dropdown :calendar/year-dropdown)

(defcomponent calendar-day :calendar/day)
(defcomponent calendar-week :calendar/week)
(defcomponent calendar-month :calendar/month)
(defcomponent calendar-time :calendar/time)
(defcomponent calendar :calendar)


(defcomponent search-field :field/search)
(defcomponent identity-field :field/identity)
(defcomponent identity-multiselect-field :field/identity-multiselect)


(defcomponent text-field :field/text)
(defcomponent integer-field :field/integer)
(defcomponent float-field :field/float)
(defcomponent currency-field :field/currency)
(defcomponent input-field :field/input)
(defcomponent dropdown-field :field/dropdown)
(defcomponent multiselect-field :field/multiselect)
(defcomponent timestamp-field :field/timestamp)
(defcomponent timestamp-period-field :field/timestamp-period)
(defcomponent date-field :field/date)
(defcomponent date-period-field :field/date-period)
(defcomponent boolean-field :field/boolean)
(defcomponent checklist-field :field/checklist)
(defcomponent idle-field :field/idle)


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
