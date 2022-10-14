(ns toddler.ui.default.table
  (:require
    [cljs-bean.core :refer [->clj]]
    [clojure.string :as str]
    goog.string
    [helix.dom :as d]
    [helix.core 
     :refer [defhook defnc memo
             create-context $
             <> provider]]
    [helix.hooks :as hooks]
    [helix.children :as c]
    [helix.placenta.util
     :refer [deep-merge]]
    [helix.styled-components
     :refer [defstyled]]
    [toddler.elements :as toddler]
    [toddler.elements.table :as table]
    [toddler.elements.popup :as popup]
    [toddler.elements.tooltip :as tip]
    [toddler.ui :as ui]
    ["react" :as react]))



(defnc NotImplemented
  []
  (let [field (use-column)] 
    (.error js/console (str "Field not implemented\n%s" (pr-str field))))
  nil)


(def $action-input
  {(str \&
        toddler/dropdown-field-wrapper 
        \space 
        toddler/dropdown-field-discard)
   {:color "transparent"}
   ;;
   (str \& toddler/dropdown-field-wrapper ":hover")
   {(str toddler/dropdown-field-discard) {:color "inherit"}}})


(defstyled uuid-cell table/UUIDCell
  (assoc tip/basic ".info-tooltip" tip/basic-content))


(defstyled enum-cell table/EnumCell
  {:input {:font-size "1em"
           :font-weight "600"
           :cursor "pointer"}})


(defstyled text-cell table/TextCell
  {:font-size "1em"
   :border "none"
   :outline "none"
   :resize "none"
   :padding 0
   :width "100%"}
  )


(defstyled timestamp-cell table/TimestampCell
  {:display "flex"
   :justify-content "center"
   :input
   {:font-size "1em"
    :border "none"
    :outline "none"
    :resize "none"
    :padding 2
    :width "100%"}}
  ;;
  )


(defstyled integer-cell table/IntegerCell
  {:border "none"
   :outline "none"
   :font-size "1em"
   :width "90%"}
  )


(defstyled float-cell table/FloatCell
  {:border "none"
   :outline "none"
   :font-size "1em"}
  )


(defstyled currency-cell table/CurrencyCell
  {:font-size "1em"
   :max-width 140
   :display "flex"
   :align-items "center"
   :input {:outline "none"
           :border "none"
           :max-width 100}}
  )

(defstyled boolean-cell table/BooleanCell
  {:font-size "1em"
   :padding 0
   :width 20
   :height 20
   :display "flex"
   :justify-content "center"
   :align-items "center"
   :transition "background-color .3s ease-in-out"}
  )


(defstyled user-cell table/UserCell 
  {:input {:font-size "1em"}
   ".clear" {:color "transparent"
             :display "flex"
             :align-items "center"}}
  )

(defstyled action-cell table/ActionCell
  {:padding 5
   :font-size "0.8em"
   :border-radius 3
   :display "flex"
   :justify-content "center"
   :transition "box-shadow .3s ease-in,background .3s ease-in"
   :border "2px solid transparent"
   :align-items "center"
   :cursor "pointer"
   ":focus" {:outline "none"}}
  )

(defstyled delete-cell table/DeleteCell
  {:display "flex"
   :justify-content "center"
   :align-content "center"
   :min-height 25
   :min-width 30
   :max-height 30
   :font-size "1em"
   :align-items "center"
   ; :margin-top 3
   ".delete-marker"
   {:cursor "pointer"
    :margin "1px 3px"
    :transition "color .3s ease-in"}}
  )

(defstyled expand-cell table/ExpandCell
  {:display "flex"
   :flex-grow "1"
   :justify-content "center"
   :cursor "pointer"
   :svg {:transition "transform .3s ease-in-out"}}
  )



;; Styled headers
(def header-style
  {:display "flex"
   :flex-direction "column"
   :font-size "1em"
   :height "100%"
   :justify-content "space-between"
   ".header" 
   {:display "flex"
    :flex-direction "row"
    ".name" {:cursor "pointer" :font-weight "600"}}
   ".filter"
   {:margin "4px 0"}})


(defstyled plain-header table/PlainHeader 
  header-style)


(defstyled user-header table/UserHeader
  (deep-merge
    header-style
    {".filter"
     {:line-height 12
      :padding 0
      ; :flex-grow "1"
      :justify-self "center"
      :resize "none"
      :border "none"
      :width "100%"}}))


(defstyled text-header table/TextHeader 
  (deep-merge
    header-style
    {".filter"
     {:line-height 12
      :padding 0
      ; :flex-grow "1"
      :justify-self "center"
      :resize "none"
      :border "none"
      :width "100%"}}))


(defstyled boolean-popup table/BooleanFilter 
  {:display "flex"
   :flex-direction "row"
   (str ui/checkbox) {:margin "1px 2px"}})


(defstyled boolean-header table/BooleanHeader 
  (deep-merge 
    header-style
    {:align-items "center"}))


(defstyled enum-popup popup/element
  {(str toddler/checklist " .name") {:font-size "1em"}})


(defstyled enum-header table/EnumHeader 
  (deep-merge
    header-style
    {:justify-content "flex-start"
     :align-items "center"
     ; ".header" {:margin-left "-1em"}
     })
  )

(defstyled timestamp-header table/TimestampHeader
  (deep-merge
    header-style
    {:justify-content "flex-start"
     :align-items "center"
     ; ".header" {:margin-left "-1em"}
     })
  )



(defstyled table table/Table
  {:display "flex"
   :flex-direction "column"
   :flex-grow "1"})


(def components
  (merge
    {:table table}
    #:cell {:expand expand-cell
            :delete delete-cell
            :boolean boolean-cell
            :currency currency-cell
            :enum enum-cell
            :float float-cell
            :hashed table/HashedCell
            :integer integer-cell
            :uuid uuid-cell
            :text text-cell
            :timestamp timestamp-cell
            :user user-cell}
    #:header {:enum enum-header
              :boolean boolean-header
              :text text-header
              :user user-header
              :timestamp timestamp-header}))
