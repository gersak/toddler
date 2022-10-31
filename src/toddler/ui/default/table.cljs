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
    ; [toddler.elements :as toddler]
    [toddler.table :as table]
    [toddler.popup :as popup]
    [toddler.tooltip :as tip]
    [toddler.ui.default.fields :as fields]
    [toddler.ui.provider :refer [ExtendUI]]
    [toddler.ui :as ui]))



(defnc NotImplemented
  []
  (let [field (table/use-column)] 
    (.error js/console (str "Field not implemented\n%s" (pr-str field))))
  nil)


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
   :transition "background-color .3s ease-in-out"})


(defstyled identity-cell-input table/IdentityCellInput
  {:display "flex"
   :align-items "center"})


(defnc IdentityCell
  [props]
  ($ ExtendUI
    {:components
     {:popup fields/identity-popup
      :input identity-cell-input}}
    ($ table/IdentityCell
       {& props})))


(defstyled identity-cell table/IdentityCell
  {:display "flex"
   :aling-items "center"
   :input {:font-size "1em"
           :outline "none"
           :border "none"}
   ".clear" {:color "transparent"
             :display "flex"
             :align-items "center"}})


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


(defstyled identity-header table/IdentityHeader
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
  {(str ui/checklist " .name") {:font-size "1em"}})


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
    #:table {:row table/Row
             :header table/HeaderRow}
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
            :identity identity-cell}
    #:header {:enum enum-header
              :boolean boolean-header
              :text text-header
              :identity identity-header
              :timestamp timestamp-header}))
