(ns toddler.themes.default
  (:require
    [helix.styled-components :as sc
     :refer [--themed]]
    [helix.placenta.util :refer [deep-merge]]
    [toddler.theme :as theme]
    [toddler.head :as head]))



(def color
  (merge
    {:gray "#616161"
     :teal "#80d6d6"
     :blue "#85b7f1"
     :red "#fd6286"
     :orange "#ff872d"
     :yellow "#ffec72"
     :green "#0dda87"
     :asphalt "#9cb4d8"
     :white "white"
     :disabled "#bbbbbb"
     :link "rgba(44,29,191,0.78)"
     
     :color "#275f82"
     :background "white"}
    #:gray {:light "#f2eff2"
            :dark "#2c2c2c"}
    #:teal {:dark "#598ea7"
            :deep "#44666d"
            :saturated "#00cccc"}
    #:asphalt {:dark "#3562a2"
               :bleached "#598ea71a"}
    #:orange {:dark "#ff4007"}
    #:yellow {:dark "#FFDC00"
              :deep "#c7ac00"}
    #:white {:transparent "#ffffffee"}))

(def level
  ["#FFFFFF"
   "#E8FAFA"
   "#D1F6F6"
   "#B9F1F1"
   "#A2ECEC"
   "#8BE8E8"
   "#74E3E3"
   "#5DDFDF"
   "#46DADA"
   "#2ED5D5"
   "#17D1D1"
   "#00CCCC"
   "#05C7C2"
   "#0AC2B8"
   "#0FBDAD"
   "#14B8A3"
   "#1AB399"
   "#1FAD8F"
   "#24A885"
   "#29A37A"
   "#2E9E70"
   "#339966"
   "#308F60"
   "#2D8659"
   "#297C53"
   "#26734C"
   "#236946"
   "#205F40"
   "#1D5639"
   "#194C33"
   "#16432C"
   "#133926"])



;;; toddler.elements
(defmethod --themed [{} 'toddler.elements/simplebar]
  [{:keys [$shadow-top $shadow-bottom $hidden]}]
  (let [box-shadow (cond-> []
                     $shadow-top (conj "inset 0px 11px 8px -10px #CCC")
                     $shadow-bottom (conj "inset 0px -11px 8px -10px #CCC"))]
    (cond-> nil
      (not-empty box-shadow)
      (assoc :box-shadow (clojure.string/join ", " box-shadow))
      $hidden
      (assoc ".simplebar-track" {:display "none"}))))


(def $element-block
  {".label"
   {:margin "2px 0 4px 4px"
    :padding-bottom 2
    :text-transform "uppercase"
    :font-size "14"
    :color (color :gray)
    :border-bottom (str "1px solid " (color :gray))}
   :padding 3})

(defmethod --themed [{} 'toddler.elements/column]
  [_]
  $element-block)

(defmethod --themed [{} 'toddler.elements/row]
  [_]
  $element-block)

(letfn [(themed-action
          [{:keys [theme context]
            _disabled :disabled}]
          (let [context (if _disabled :disabled context)
                _color "#7c7c7c"
                hover "#595959"]
            (case (:name theme)
              {:color (if _disabled (color :disabled) _color)
               ":hover" {:color (case context
                                  :positive (color :green)
                                  :negative (color :red)
                                  :fun (color :teal/saturated)
                                  :fresh (color :yellow)
                                  :stale (color :gray/light)
                                  :disabled (color :disabled)
                                  hover)}
               :cursor (if _disabled "default" "pointer")
               :pointer-events (if _disabled "none" "inherit")})))]
  (defmethod --themed [{} 'toddler.elements/action] [props] (themed-action props))
  (defmethod --themed [{} 'toddler.elements/named-action] [props] (themed-action props)))


(defn button-colors
  [{:keys [context disabled]}]
  (let [context (if disabled :disabled context)]
    (case context
      ;;
      :positive
      {:color "white"
       :background-color (color :green/dark)
       :hover {:color "white"
               :background-color (color :green)}}
      ;;
      :negative
      {:color (color :red)
       :background-color (color :gray/light)
       :hover {:color "white"
               :background-color (color :red)}}
      ;;
      :fun
      {:color "white"
       :background-color (color :teal)
       :hover {:background-color (color :teal/saturated)}}
      ;;
      :fresh
      {:color (color :gray)
       :background-color (color :gray/light)
       :hover {:color "black"
               :background-color (color :yellow)}}
      ;;
      :stale
      {:color (color :gray)
       :background-color (color :gray/light)
       ::hover {:color "white"
                :background-color (color :gray/dark)}}
      ;;
      :disabled
      {:color "white"
       :background-color "#bbbbbb"
       :cursor "initial"}
      ;;
      {:color "white"
       :background-color "#5e82b8"
       :hover {:background-color (color :asphalt/dark)}})))


(defn interactions-drop-on-active
  [_]
  {":active" {:transform "translate(0px,2px)" :box-shadow "none"}})

(defn interactions-button
  [{:keys [disabled] :as props}]
  (let [{:keys [background-color color hover]}
        (button-colors props)]
    (cond->
     {:color color 
      :background-color background-color 
      ":hover" (assoc hover :box-shadow "0px 2px 4px 0px #aeaeae")}
      disabled (assoc :pointer-events "none"))))

(defmethod --themed [{} 'toddler.elements/button] [props]
  (merge (interactions-button props) (interactions-drop-on-active props)))


(defmethod --themed [{} 'toddler.elements/checkbox-button]
  [{:keys [theme $active disabled]}]
  (let [[c bc] (case (:name theme)
                 ["white" (case $active
                            true (color :green)
                            false (color :disabled)
                            (color :gray/light))])]
    (cond->
     {:color c
      :background-color bc}
      disabled (assoc :pointer-events "none"))))


(defmethod --themed [{} 'toddler.elements/checkbox-field]
  [_]
  {:color (color :gray)})


(defmethod --themed [{} 'toddler.elements/slider]
  [{:keys [width height orient]
    {twidth :width theight :height
     :or {twidth 12 theight 12}} :thumb
    :or {width "100%"
         height 5}}]
  (cond->
   {:width width :height height
    :background-color (color :gray/light)
    "&::-webkit-slider-runnable-track"
    {:border-radius 4}
    "&::-moz-range-track"
    {:border-radius 4}
    "&::-webkit-slider-thumb"
    {:-webkit-appearance "none"
     :appearance "none"
     :width twidth :height theight
     :background (color :gray)
     :border-radius "50%"
     :cursor "pointer"}
    "&::-moz-range-thumb"
    {:width twidth :height theight
     :background (color :gray)
     :cursor "pointer"}
    ":hover::-webkit-slider-thumb"
    {:background (color :gray/dark)}}
    ;;
    (= orient "vertical")
    (as-> css
          (let [hw (/ width 2)]
            (assoc css
                   :transform (str "translate(" (* -1 hw) "px,0) rotate(270deg)")
                   :margin-top hw)))
    #_(assoc
       :writing-mode "bt-lr"
       :-webkit-appearance "slider-vertical")))


(defmethod --themed [{} 'toddler.elements/dropdown-option]
  [{:keys [theme selected]}]
  (let [[c h b bh]
        (case theme
          (let [c "#00b99a"
                h "#d7f3f3"]
            [(if selected c (color :gray)) (color :gray) "white" h]))]
    {:color c
     :cursor "pointer"
     :background-color b
     :transition "color .2s ease-in,background-color .2s ease-in"
     :padding "4px 6px 4px 4px"
     ; :border-radius 3
     ; :font-weight "500" 
     " :hover" {:color h
                :background-color bh}
     ":last-child" {:border-bottom "none"}}))

(def $dropdown-shadow
  {:background-color "white"
   :box-shadow "0px 3px 10px -3px black"
   " .simplebar-scrollbar:before"
   {:background (color :gray)
    :pointer-events "none"}})



(def $with-delete-marker
  {".tbody .trow"
   {":hover"
    {" .delete-marker"
     {:opacity "1 !important"
      :transition "opacity .6s ease-in-out"}}
    " .delete-marker:not(.selected)" {:opacity "0"}}})

(def $zebra-table
  {".tbody .trow"
   {:min-height 30
    "&.odd"
    {:background-color "#f8f8ff"}
    "&.even"
    {:background-color "#ececff"}
    "&:first-child" {:padding-top 6}
    "&:last-child" {:padding-bottom 6}
    "&:first-child:last-child" {:padding "6px 0"}}})

(def $rounded-table
  {".thead"
   {:box-sizing "border-box"
    ".trow" {:padding-bottom 8}}
   ".tbody"
   {:box-sizing "border-box"
    :border-radius 13}
   ; ".tbody:not(.empty)" {:box-shadow "1px 3px 7px 2px #00000033"}
   ".tbody:not(.empty)" {:box-shadow "1px 4px 11px 1px #0000001f"}
   ".tbody .simplebar-content-wrapper"
   {:border-radius 13}
   ".tbody .trow"
   {:box-sizing "border-box"
    "&:first-child" {:border-radius "13px 13px 0px 0px"}
    "&:last-child" {:border-radius "0px 0px 13px 13px"}
    "&:first-child:last-child" {:border-radius 4}}})


(def $dropdown
  {:max-height 400})

(defmethod --themed [{} 'toddler.elements/dropdown-popup]
  [_]
  (deep-merge
   $dropdown-shadow
   $dropdown))


(defmethod --themed [{} 'toddler.elements/dropdown-element-decorator]
  [_]
  {"&.opened" {:color "transparent"}
   :cursor "default"})

(defmethod --themed [{} 'toddler.elements/dropdown-field-discard]
  [_]
  {":hover" {:color (str (color :red) "!important")}})

(defmethod --themed [{} 'toddler.elements/calendar-day]
  [_]
  {".day"
   {:color (color :gray)
    :font-size 12
    ;;
    "&.disabled, &:hover.disabled"
    {:background-color "white"
     :color (color :disabled)
     :border-color "transparent"
     :cursor "default"}
    ;;
    ":hover:not(.empty),&.selected"
    {:color "white"
     :background-color (color :teal/saturated)
     :border-collapse "collapse"
     :border (str "1px solid " (color :teal/dark))
     :border-radius 2
     :font-weight 500}
    "&.today"
    {:border "1px solid (color :teal)"}
    "&.weekend"
    {:color (color :red)}}})


(defmethod --themed [{} 'toddler.elements/calendar-month-header]
  [_]
  {".day-wrapper .day"
   {:color (color :gray)
    :font-size 12
    "&.weekend"
    {:color (color :red)}}})

(defmethod --themed [{} 'toddler.elements/calendar-month]
  [_]
  {".week-days-header .day-wrapper .day"
   {:color (color :gray)}
   ".week-row .week-days .day-wrapper .day"
   {:color (color :gray)
    ;;
    "&.disabled, &:hover.disabled"
    {:background-color "white"
     :color (color :disabled)
     :border-color "transparent"
     :cursor "default"}
    ;;
    ":hover:not(.empty),&.selected"
    {:color "white"
     :background-color (color :teal/saturated)
     :border-collapse "collapse"
     :border (str "1px solid " (color :teal/dark))
     :border-radius 2
     :font-weight 500}}})

(defmethod --themed [{} 'toddler.elements/default-field]
  [{_disabled :disabled}]
  {".field-name"
   {:color (cond
             _disabled (color :disabled)
             :else (color :gray))
    :user-select "none"
    :transition "all .3s ease-in-out"
    :font-size 12
    :font-weight "600"
    :text-transform "uppercase"}})

(defmethod --themed [{} 'toddler.elements/field-wrapper]
  [{:keys [context]}]
  (case context
    {:border-color "#b3b3b3 !important"
     :background "#e5e5e5"
     :transition "all .3s ease-in-out"
     ":focus-within" {:border-color (str (color :teal) "!important")
                      :box-shadow (str "0 0 3px " (color :teal))
                      :background-color "transparent"}
     "input,textarea"
     {:color (color :gray)}}))


(defmethod --themed [{} 'toddler.elements/search]
  [_]
  {:border-color "#b3b3b3 !important"
   :background "#eaeaea"
   :transition "all .3s ease-in-out"
   ".icon" {:transition "all .3s ease-in-out"
            :color "#7c7c7c"}
   ":focus-within" {:border-color (str (color :teal) "!important")
                    :box-shadow (str "0 0 3px " (color :teal))
                    :background-color "transparent"
                    ".icon" {:color (color :teal)}}
   "input"
   {:color (color :gray)}})

(defmethod --themed [{} 'toddler.elements/dropdown-wrapper]
  [{:keys [disabled]}]
  (when disabled
    {:pointer-events "none"}))
(defmethod --themed [{} 'toddler.elements/dropdown-field-wrapper] [_] {:color "#adacac"})

(defmethod --themed [{} 'toddler.elements/multiselect-wrapper]
  [{:keys [opened]}]
  {:cursor (if opened "default" "pointer")})


(defmethod --themed [{} 'toddler.elements/timestamp-clear] [_]
  {:background-color (color :gray/light)
   :color "white"
   :transition "background .3s ease-in-out"
   :border-radius 20
   :cursor "pointer"
   ":hover" {:background-color (color :red)}})


(defmethod --themed [{} 'toddler.elements/time-period-popup] [_]
  $dropdown-shadow)


(defmethod --themed [{} 'toddler.elements/user]
  [{:keys [disabled read-only]}]
  (let [cursor (if (or disabled read-only)
                 "default"
                 "pointer")]
    (cond->
     {:color (color :gray)
      :cursor cursor
      :input {:cursor cursor}}
      (or disabled read-only) (assoc :pointer-events "none"))))



(defmethod --themed [{} 'toddler.elements/group]
  [{:keys [disabled read-only]}]
  (let [cursor (if (or disabled read-only)
                 "default"
                 "pointer")]
    (cond->
     {:color (color :gray)
      :cursor cursor
      :input {:cursor cursor}}
      (or disabled read-only) (assoc :pointer-events "none"))))


(defmethod --themed [{} 'toddler.elements/user-multiselect]
  [{:keys [disabled read-only]}]
  (let [cursor (if (or disabled read-only)
                 "default"
                 "pointer")]
    (cond->
     {:color (color :gray)
      :cursor cursor
      :input {:cursor cursor}}
      (or disabled read-only) (assoc :pointer-events "none"))))


(defmethod --themed [{} 'toddler.elements/group-multiselect]
  [{:keys [disabled read-only]}]
  (let [cursor (if (or disabled read-only)
                 "default"
                 "pointer")]
    (cond->
     {:color (color :gray)
      :cursor cursor
      :input {:cursor cursor}}
      (or disabled read-only) (assoc :pointer-events "none"))))


(defmethod --themed [{} 'toddler.elements/user-cell]
  [_]
  {:input {:color color}})



(defmethod --themed [{} 'toddler.elements/table-header]
  [_]
  {:border-color (color :teal/dark)})


(defn interactive-cell? [{:keys [cell/disabled cell/read-only]}]
  (every? not [disabled read-only]))

(defmethod --themed [{} 'toddler.elements/timestamp-cell]
  [{:keys [opened] :as props}]
  (let [interactive? (interactive-cell? props)]
    {".decorator" {:color (if (or (not interactive?) opened) "transparent" "#adacac")
                   :user-events "none"}
     ".time" {:input {:color (color :gray/dark)}}
     :cursor (if interactive? "pointer" "default")
     :input {:cursor (if interactive? "pointer" "default")}}))



(defmethod --themed [{} 'toddler.elements/table-row] [_]
  {":hover" {:border-color (color :teal/dark)}})

(defmethod --themed [{} 'toddler.elements/table-header-cell] [_]
  {:color (color :gray)})


;; toddler.elements.light
(defmethod --themed [{} 'toddler.elements.light/field-input]
  [{:keys [readOnly]}]
  (cond->
   {:color (color :text)
    :transition "border-color 0.2s ease-in"
    :border-bottom (str "1px solid " (color :asphalt/bleached))
    "&::placeholder" {:color (color :asphalt)}
    "&:focus,&:hover"
    {:border-color (color :text)}}
    readOnly (assoc :cursor "pointer")))


(defmethod --themed [{} 'toddler.elements.light/search]
  [{:keys [position]
    :or {position :right}}]
  (let [[c bc pc] [color (color :asphalt/dark) (color :asphalt)]]
    {:color c
     :input
     {:color c
      "&::placeholder" {:color pc}
      "&:focus::placeholder" {:color c}
      :border-bottom (str "1px solid " bc)}
     :justify-content (case position
                        :left "flex-start"
                        :center "center"
                        "flex-end")}))


(defmethod --themed [{} 'toddler.elements.light/dropdown-option]
  [{:keys [selected]}]
  {:color (if selected
            (color :teal/saturated)
            "#dbdbdb")
   :cursor "pointer"
   :transition "color .2s ease-in,background-color .2s ease-in"
   :padding "4px 6px 4px 4px"
   " :hover" {:color "white"
              :text-shadow "0 0 8px #ffffff99, 0 0 8px #ffffff99"}
   ":last-child" {:border-bottom "none"}})


(letfn [(--themed-action
          [{:keys [context disabled]}]
          (let [context (if disabled :disabled context)]
            (cond->
             {:color color
              ":hover" {:color (case context
                                 :positive (color :green/pastel)
                                 :negative (color :red)
                                 :fun (color :teal/saturated)
                                 :fresh (color :yellow)
                                 :stale (color :gray/light)
                                 :disabled (color :disabled)
                                 color)}
              :cursor (if disabled "default" "pointer")}
              disabled (assoc
                        :visibility "hidden"
                        :cursor "default"
                        :pointer-events "none"))))]
  (defmethod --themed [{} 'toddler.elements.light/action] [props] (--themed-action props))
  (defmethod --themed [{} 'toddler.elements.light/named-action] [props] (--themed-action props)))


(defmethod --themed [{} 'toddler.elements.light/tabs]
  [_]
  (let [[c ic bc bcl]
        [(color :color) (color :asphalt) (color :color) (color :asphalt)]]
    {:box-shadow "0px 2px 4px -4px black"
     :border-bottom (str "1px solid " bc)
     " .tab"
     {:color ic
      "&:not(:first-child)"
      {:border-left (str "1px solid " bcl)}
      "&.selected, &:hover"
      {:color c
       :transition "color .3s ease-in"
       :cursor "default"}}}))


(defmethod --themed [{} 'toddler.elements/textarea-field]
  [_]
  {:textarea {:font-family "Roboto"}})

(defmethod --themed [{} 'toddler.elements.light/user-dropdown-input]
  [_]
  {"input::placeholder" {:color "#8fa7c4"}
   :input {:color color}})


(letfn [(--editable-tag [{:keys [editable?]}]
          (when-not editable?
            {:user-select "none"}))

        (--colored-tag
          [{:keys [theme context]}]
          (let [[c b rc rch]
                (case theme
                  (case context
                    :positive ["white" (color :green) (color :green/dark) "black"]
                    :negative ["white" (color :red) (color :red/dark) "black"]
                    :fun ["white" (color :teal/saturated) (color :teal/dark) "black"]
                    :fresh ["black" (color :yellow) (color :yellow/dark) "black"]
                    :stale ["white" (color :gray) (color :gray/dark) "black"]
                    :exception ["white" "black" (color :gray) (color :red)]
                    ["white" (color :teal) (color :teal/dark) (color :red)]))]
            {:color c
             :background-color b
             " .remove" {:color rc
                         :cursor "pointer"
                         :transition "color .2s ease-in"
                         :path {:cursor "pointer"}}
             " .remove:hover" {:color rch}}))]
  (defmethod --themed [{} 'toddler.elements.light/user-tag]
    [props]
    (deep-merge
     {:img {:margin-right 4}}
     (--editable-tag props)
     (--colored-tag props)))

  (defmethod --themed [{} 'toddler.elements.light/tag]
    [props]
    (deep-merge
     (--editable-tag props)
     (--colored-tag props)))

  (defmethod --themed [{} 'toddler.elements/user-tag]
    [props]
    (deep-merge
     {:img {:margin-right 4}}
     (--editable-tag props)
     (--colored-tag props)))

  (defmethod --themed [{} 'toddler.elements/tag]
    [props]
    (deep-merge
     (--editable-tag props)
     (--colored-tag props))))



(defmethod --themed [{} 'toddler.elements.modal/close-button] [_]
  {:color "#bfbfbf"
   "&:hover" {:color (color :red)}})


(defmethod --themed [{} 'toddler.elements.modal/feedback-footer] [_]
  {".warning" {:color (color :orange)}
   ".error" {:color (color :red)
             :svg {:margin-right 6}}})

(defmethod --themed [{} 'toddler.elements.tabs/tab-container] [_]
  (let [[c ic bc bcl]
        [(color :gray/dark) "#b4b4b4" (color :gray/dark) "#b4b4b4"]]
    {:border-bottom (str "1px solid " bc)
     " .tab"
     {:color ic
      "&:not(:first-child)"
      {:border-left (str "1px solid " bcl)}
      "&.selected, &:hover"
      {:color c
       :transition "color .3s ease-in"
       :cursor "default"}}}))


(defmethod --themed [{} 'toddler.elements.tabs/tab-container]
  [{:keys [selected]}]
  {:color "white"
   :background (color :asphalt/dark)
   :opacity (if selected "1" "0.2")
   :border "2px solid #d2dfec"
   ":hover"
   {:opacity "1"}
   ".negative"
   {:border "2px solid #ffe0e7"
    :background-color (color :red)
    :opacity "0.2"
    ":hover"
    {:opacity "1"}}
   ".positive"
   {:border "2px solid #d9f7e7"
    :opacity "0.2"
    ":hover"
    {:opacity "1"}}
   ".disabled"
   {:cursor "default"
    :opacity "0.2 !important"}})


(defmethod --themed [{} 'toddler.elements.tabs/tab-container]
  [_]
  $dropdown-shadow)


(defmethod --themed [{} 'toddler.elements.avatar/avatar-dropzone]
  []
  {:color (color :gray)
   :label {":hover" {:color (color :gray/dark)}}})


(defmethod --themed [{} 'toddler.elements.popup/element]
  []
  $dropdown-shadow)


(defmethod --themed [{} 'toddler.elements.drawers/drawer-action]
  [{:keys [selected]}]
  {:color "white"
   :background (color :asphalt/dark)
   :opacity (if selected "1" "0.2")
   :border "2px solid #d2dfec"
   ":hover"
   {:opacity "1"}
   ".negative"
   {:border "2px solid #ffe0e7"
    :background-color (color :red)
    :opacity "0.2"
    ":hover"
    {:opacity "1"}}
   ".positive"
   {:border "2px solid #d9f7e7"
    :opacity "0.2"
    ":hover"
    {:opacity "1"}}
   ".disabled"
   {:cursor "default"
    :opacity "0.2 !important"}})


(defmethod --themed [{} 'toddler.elements/card-action]
  [{:keys [context]}]
  {:background-color (color :white)
   ".action:hover"
   (case context
     :negative
     {:background-color (color :red)
      :color "#fff8f3"}
     {:background-color (color :teal)
      :color "#fff8f3"})
   ".action"
   {:background-color "#929292"
    :color (color :gray/light)}})


(def global (sc/import-resource "css/toddler.css"))
(def simplebar (sc/import-resource "css/simplebar.css"))

(head/add
  :link
  {:href "https://fonts.googleapis.com/css2?family=Roboto&display=swap"
   :rel "stylesheet"})
