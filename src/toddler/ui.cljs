(ns toddler.ui
  (:refer-clojure :exclude [identity])
  (:require-macros [toddler.ui :refer [defcomponent]])
  (:require
    ["react" :as react]
    [helix.core :refer [create-context]]))


(def ^js __components__ (create-context))
(def forward-ref react/forwardRef)


(defcomponent avatar :avatar)
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
(defcomponent field :field)
(defcomponent wrapper :wrapper)
(defcomponent discard :discard)
(defcomponent dropdown :dropdown)
(defcomponent img :img)
(defcomponent header :header)
(defcomponent identity :identity)
(defcomponent tooltip tooltip)
(defcomponent autosize-input :input/autosize)
(defcomponent idle-input :input/idle)
(defcomponent action :action)
(defcomponent checklist :checklist)


(defcomponent card :card)
(defcomponent card-action :card/action)
(defcomponent card-actions :card/actions)


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
(defcomponent input-field :field/input)
(defcomponent dropdown-field :field/dropdown)
(defcomponent multiselect-field :field/multiselect)
(defcomponent timestamp-field :field/timestamp)
(defcomponent period-field :field/period)
(defcomponent boolean-field :field/boolean)


(defcomponent table :table)
(defcomponent table-row :table/row)
(defcomponent table-header-row :table/header-row)


(defcomponent enum-header :header/enum)
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
