(ns toddler.elements.table.theme
  (:require
   clojure.string
   [toddler.theme
    :refer [pastel-green
            red
            saturated-teal
            yellow
            light-gray
            disabled
            green
            color
            asphalt
            dark-asphalt
            level
            gray
            interactions-button
            interactions-drop-on-active]]
   [helix.styled-components :as sc :refer [--themed]]
   [helix.placenta.util :refer [deep-merge]]))


(letfn [(--themed-action 
          [{:keys [theme context disabled]}]
          (let [context (if disabled :disabled context)]
            (case (:name theme)
              (cond->
                {:color gray
                 ":hover" {:color (case context
                                    :positive pastel-green
                                    :negative red
                                    :fun saturated-teal
                                    :fresh yellow
                                    :stale light-gray
                                    :disabled disabled
                                    gray)}
                 :cursor (if disabled "default" "pointer")}
                disabled (assoc
                           :visibility "hidden"
                           :cursor "default"
                           :pointer-events "none")))))]

  (defmethod --themed [{} 'neyho.eywa.table/action] [props]
    (--themed-action props))
  (defmethod --themed [{} 'toddler.elements.table/action] [props]
    (--themed-action props)))

;; Datasource Table
(let [$themed-cell
      {:color gray
       "input,textarea"
       {:color gray}}]
  (defmethod --themed [{} 'toddler.elements.table/uuid-cell] [_]
    {"&.copied " {:color green}})
  (defmethod --themed [{} 'toddler.elements.table/enum-cell] 
    [_]
    (assoc $themed-cell
           :margin-top 2
           "&.opened" {:input {:color saturated-teal}}
           "input,textarea" {:color color}))
  (defmethod --themed [{} 'toddler.elements.table/text-cell] [_]
    (assoc $themed-cell :margin-top 3))

  (defmethod --themed [{} 'toddler.elements.table/timestamp-cell] [_]
    (assoc $themed-cell :margin-top 2))

  (defmethod --themed [{} 'toddler.elements.table/integer-cell] [_]
    (assoc $themed-cell :margin-top 2))

  (defmethod --themed [{} 'toddler.elements.table/boolean-cell] [_]
    (assoc $themed-cell :margin-top 2))

  (defmethod --themed [{} 'toddler.elements.table/currency-cell] [_] $themed-cell)
  
  (defmethod --themed [{} 'toddler.elements.table/user-cell]  [_]
    {:input {:color color}
     "&:hover .clear" {:color gray
                       "&:hover" {:color red}}})
  (defmethod --themed [{} 'toddler.elements.table/delete-cell]
    [_]
    {".delete-marker"
     {:color asphalt
      "&:hover, &.selected" {:color red}}})
  (defmethod --themed [{} 'toddler.elements.table/expand-cell]
    [_]
    {".icon"
     {:color dark-asphalt 
      "&.expanded" {:transform "rotate(90deg)"}
      ":hover" {:color dark-asphalt}}})
  (defmethod --themed [{} 'toddler.elements.table/plain-header] [_] 
    (assoc $themed-cell ".header"
           {:color gray
            :text-transform "uppercase"}))
  (defmethod --themed [{} 'toddler.elements.table/boolean-header] [_] 
    (deep-merge 
      {"&.active, .active" 
       {:color color
        :background-color "transparent"}
       "&.inactive, .inactive" 
       {:color "#a7a7a79e"
        :background-color "transparent"}}
      $themed-cell))
  (defmethod --themed [{} 'toddler.elements.table/boolean-popup]
    [_]
    {".active" {:color color}
     ".inactive" {:color "#a7a7a79e"}
     :svg {:margin 3}})
  (defmethod --themed [{} 'toddler.elements.table/user-header] [_] $themed-cell)
  (defmethod --themed [{} 'toddler.elements.table/enum-header] [_] $themed-cell)
  (defmethod --themed [{} 'toddler.elements.table/timestamp-header] [_] $themed-cell)
  (defmethod --themed [{} 'toddler.elements.table/text-header] [_] $themed-cell)
  (defmethod --themed [{} 'toddler.elements.table/pin-level] [_]
    (reduce
      (fn [s idx]
        (assoc s
          (str "&[level=\"" idx "\"]")
          {:background-color (get level idx)}))
      nil 
      (range (count level))))


  (defmethod --themed [{} 'toddler.elements.table/action-cell] [props]
    (deep-merge
      (interactions-button props)
      (interactions-drop-on-active props)))


  (defmethod --themed [{} 'toddler.elements.table/pagination] [_]
    {:color gray
     :button 
     {:background-color "transparent"
      :color disabled
      ":disabled" {:cursor "initial" :pointer-events "none"}
      ":hover:enabled" {:color gray}}
     :select 
     {:color gray
      :border "none"
      :background-color "white"
      :option 
      {:color "black"
       :background-color "white"}}
     :input 
     {:color gray}})

  (defmethod --themed [{} 'toddler.elements.table/table] 
    [_]
    {".tbody .trow"
     {:box-sizing "border-box",
      "&:first-child:last-child" {:border-radius 4},
      :min-height 30,
      "&:last-child" {:border-radius "0px 0px 4px 4px"},
      "&.odd" {:background-color "#ceeef8"},
      ":hover"
      {" .delete-marker"
       {:opacity "1 !important", :transition "opacity .6s ease-in-out"}},
      " .delete-marker:not(.selected)" {:opacity "0"},
      :padding "4px 0",
      "&.even" {:background-color "#b9dbe7"},
      "&:first-child" {:border-radius "4px 4px 0px 0px"}},
     ".thead" {:box-sizing "border-box"},
     ".tbody" {:box-sizing "border-box", :border-radius 6},
     ".tbody:not(.empty)" {:box-shadow "1px 4px 11px 1px #0000001f"},
     ".tbody .simplebar-content-wrapper" {:border-radius 6}}))


