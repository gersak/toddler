(ns toddler.ui.default.table
  (:require
    goog.string
    [clojure.string :as str]
    [helix.dom :as d]
    [helix.core 
     :refer [defnc $ provider defhook]]
    [helix.hooks :as hooks]
    [helix.children :as c]
    [shadow.css :refer [css]]
    ; [toddler.elements :as toddler]
    [toddler.hooks :refer [use-delayed
                           use-translate
                           use-dimensions]]
    [toddler.table :as table]
    [toddler.popup :as popup]
    [toddler.layout :as layout]
    ; [toddler.tooltip :as tip]
    [toddler.dropdown :as dropdown]
    [toddler.input :refer [TextAreaElement]]
    [toddler.ui.default.elements :as e]
    [toddler.ui :as ui]
    ["toddler-icons$default" :as icon]))



(defnc NotImplemented
  []
  (let [field (table/use-column)] 
    (.error js/console (str "Field not implemented\n%s" (pr-str field))))
  nil)


(defnc row
  [{:keys [className] :as props} _ref]
  {:wrap [(ui/forward-ref)]}
  ($ table/Row
    {:ref _ref
     :className (str/join
                  " "
                  (remove
                    empty?
                    [className
                     (css
                       :my-1
                       {:min-height "2em"})]))

     & (dissoc props :className :class)}))


(defnc uuid-cell
  []
  (let [[visible? set-visible!] (hooks/use-state nil)
        hidden? (use-delayed (not visible?) 300)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)
        [copied? set-copied!] (hooks/use-state false)
        column (table/use-column)
        [value] (table/use-cell-state column)
        $button (css :w-7 :p-1
                     :text-white
                     :rounded-sm
                     :flex
                     :justify-center
                     :items-center
                     :bg-cyan-500)
        $popup (css
                 :shadow-md)
        $tooltip (css
                   :flex
                   :justify-start
                   :wrap
                   :rounded-lg
                   :text-white
                   :p-4
                   :shadow-lg
                   :font-bold
                   {:background-color "#333333ee"})
        $copied (css
                  :text-green-300)]
    ($ popup/Area
       {:ref area
        :className (css
                     {:padding-top "0.4em"})
        :onMouseLeave (fn [] (set-visible! false))
        :onMouseEnter (fn [] 
                        (set-copied! nil)
                        (set-visible! true))}
       (d/button
         {:className $button
          :context :fun
          :onClick (fn [] 
                     (when-not copied?
                       (.writeText js/navigator.clipboard (str value))
                       (set-copied! true)))}
         ($ icon/uuid))
       (when visible?
         ($ popup/Element
            {:ref popup
             :style {:visibility (if hidden? "hidden" "visible")
                     :animationDuration ".5s"
                     :animationDelay ".3s"}
             :preference popup/cross-preference
             :className $popup}
            (d/div
              {:class (cond-> [$tooltip]
                      copied? (conj $copied))}
              (str value)))))))


(defnc enum-cell
  []
  (let [{:keys [placeholder label] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        ;;
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [input
                search
                on-key-down
                sync-search!
                on-change
                disabled
                toggle!
                area]
         :as dropdown}
        (dropdown/use-dropdown
          (merge
            {:search-fn :name
             :ref-fn :value
             :searchable? true
             :onChange set-value!}
            (assoc column
                   :value value
                   :area-position area-position)))]
    (provider
      {:context popup/*area-position*
       :value [area-position set-area-position!]}
      (provider
        {:context dropdown/*dropdown*
         :value dropdown}
        ($ popup/Area
           {:ref area
            :onClick toggle!
            :className (css
                         :flex
                         :justify-start
                         :text-sm)}
           (d/div
             {:className (css
                           :flex
                           :items-center
                           :py-2
                           :font-bold
                           ["& .decorator"
                            :text-transparent
                            {:transition "color .2s ease-in-out"
                             :position "absolute"
                             :right "0px"}]
                           ["&:hover .decorator" :text-gray-400])}
             (d/input
               {:ref input
                :className "input"
                :value search 
                :disabled disabled
                :spellCheck false
                :auto-complete "off"
                :placeholder (or placeholder label)
                :onChange on-change 
                :onBlur sync-search!
                :onKeyDown on-key-down})
             (d/span
               {:className "decorator"}
               ($ icon/dropdownDecorator)))
           ($ dropdown/Popup
              {:className "dropdown-popup"
               :render/option e/dropdown-option
               :render/wrapper e/dropdown-wrapper}))))))


(defnc text-cell
  []
  (let [{:keys [label placeholder options read-only] 
         {:keys [width]} :style
         :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        $text (css
                :text-sm
                :outline-none
                :py-2
                :w-full
                {:resize "none"})] 
    ($ TextAreaElement
       {:value value 
        :class $text
        :read-only read-only
        :spellCheck false
        :auto-complete "off"
        :style {:maxWidth width}
        :onChange (fn [e] 
                    (set-value! 
                      (not-empty (.. e -target -value))))
        :options options 
        :placeholder (or placeholder label)})))



(defnc timestamp-cell
  []
  (let [{:keys [placeholder label format disabled read-only show-time] :as column} (table/use-column)
        format (or format (if show-time :datetime :date))
        [value set-value!] (table/use-cell-state column)
        [opened set-opened!] (hooks/use-state false)
        translate (use-translate)
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)]
    (popup/use-outside-action
      opened area popup
      (fn [e]
        (when (.contains js/document.body (.-target e))
          (set-opened! false))))
    ($ popup/Area
       {:ref area
        :onClick (fn []
                   (when (and (not disabled) (not read-only))
                     (set-opened! true)))
        :className (css
                     ; :flex
                     ; :grow
                     :items-center
                     :py-2
                     {:display "flex"
                      :flex-grow "1"}
                     ["& .clear"
                      :text-transparent
                      {:transition "color .2s ease-in-out"
                       :position "absolute"
                       :right "0px"}]
                     ["&:hover .clear " :text-gray-400]
                     ["& .clear:hover" :text-gray-900 :cursor-pointer])}
       (d/input
         {:className (css :text-sm)
          :readOnly true
          :value (if (some? value) (translate value format) "")
          :spellCheck false
          :auto-complete "off"
          :disabled disabled
          :placeholder (or placeholder label)})
       (when value
         (d/span
           {:class (cond-> ["clear"]
                     opened (conj "opened"))
            :onClick (fn [e]
                       (.stopPropagation e)
                       (.preventDefault e)
                       (set-value! nil)
                       (set-opened! false))}
           ($ icon/clear)))
       (when opened
         ($ popup/Element
            {:ref popup
             ; :className className 
             :wrapper e/dropdown-wrapper
             :preference popup/cross-preference}
            ($ e/timestamp-calendar
               {:value value
                :disabled disabled
                :read-only read-only
                :onChange (fn [x]
                            (set-value! x)
                            (when-not show-time (set-opened! false)))})
            (when show-time
              ($ e/timestamp-time
                 {:value value
                  :disabled (if-not value true
                              disabled)
                  :read-only read-only
                  :onChange set-value!})))))))


(letfn [(text->number [text]
          (if (empty? text) nil
            (let [number (js/parseInt text)]
              (when-not (js/Number.isNaN number) number))))]
  (defnc integer-cell
    []
    (let [{:keys [placeholder read-only disabled label] :as column} (table/use-column)
          [value set-value!] (table/use-cell-state column)
          translate (use-translate)
          [focused? set-focused!] (hooks/use-state false)
          [input set-input!] (hooks/use-state (when value (translate value)))
          $input (css
                   :outline-none
                   :border-0
                   :text-sm
                   :w-full
                   :py-2)]
      (hooks/use-effect
        [focused?]
        (set-input! (str value)))
      (hooks/use-effect
        [value]
        (when-not (= (text->number input) value)
          (set-input! (when value (translate value)))))
      (d/input
        {:value (if value
                  (if focused? 
                    input
                    (translate value))
                  "")
         :class [$input]
         :placeholder (or placeholder label)
         :read-only read-only
         :disabled disabled
         :onFocus #(set-focused! true)
         :onBlur #(set-focused! false)
         :onChange (fn [e]
                     (let [text (.. e -target -value)
                           number (js/parseInt text)]
                       (if (empty? text) (set-value! nil)
                         (when-not (js/Number.isNaN number)
                           (when-not (= number value) (set-input! text))
                           (set-value! number)))))}))))


(letfn [(text->number [text]
          (if (empty? text) nil
            (let [number (js/parseFloat text)]
              (when-not (js/Number.isNaN number) number))))]
  (defnc float-cell
    []
    (let [{:keys [placeholder read-only disabled label] :as column} (table/use-column)
          [value set-value!] (table/use-cell-state column)
          translate (use-translate)
          [input set-input!] (hooks/use-state (when value (translate value)))
          [focused? set-focused!] (hooks/use-state false)
          $float (css
                   :border-0
                   :outline-0
                   :text-sm
                   :py-2)]
      (hooks/use-effect
        [focused?]
        (set-input! (str value)))
      (hooks/use-effect
        [value]
        (when-not (= (text->number input) value)
          (set-input! (when value (translate value)))))
      (d/input
        {:className $float
         :value (if value
                  (if focused? 
                    input
                    (translate value))
                  "")
         :placeholder (or placeholder label)
         :read-only read-only
         :disabled disabled
         :onFocus #(set-focused! true)
         :onBlur #(set-focused! false)
         :onChange (fn [e]
                     (let [text (str/replace
                                  (.. e -target -value)
                                  #"[\.\,]+" ".")
                           number (js/parseFloat text)
                           dot? (#{\.} (last text))]
                       (if (empty? text) (set-value! nil)
                         (when-not (js/Number.isNaN number)
                           (when (or (not= number value) dot?)
                             (set-input! text))
                           (when-not dot? (set-value! number))))))}))))



(defnc currency-cell
  []
  nil)


(defnc boolean-cell
  []
  (let [{:keys [read-only disabled] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        $button (css
                  :text-sm
                  :w-5
                  :h-5
                  :flex
                  :rounded-sm
                  :justify-center
                  :items-center
                  {:transition "background-color .3s ease-in-out"})
        $active (css :bg-green-400
                     :text-white)
        $inactive (css :bg-gray-200
                       :text-white)]
    (d/div
      {:className (css
                    :justify-center
                    :py-2)}
      (d/button
        {:disabled disabled
         :read-only read-only
         :class (cond->
                  [$button]
                  value (conj $active)
                  (not value) (conj $inactive))
         :onClick #(set-value! (not value))}
        ($ (case value
             nil icon/checkboxDefault
             icon/checkbox))))))


(defnc identity-cell
  []
  (let [{:keys [placeholder] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [input
                search
                on-key-down
                sync-search!
                disabled
                read-only
                searchable?
                toggle!
                area]
         :or {searchable? true}
         :as dropdown}
        (dropdown/use-dropdown (assoc column
                                      :value value
                                      :area-position area-position
                                      :search-fn :name))]
    (provider
      {:context popup/*area-position*
       :value [area-position set-area-position!]}
      (provider
        {:context dropdown/*dropdown*
         :value dropdown}
        ($ popup/Area
           {:ref area
            :onClick toggle!
            :className (css
                         :flex
                         :justify-start
                         :text-sm)}
           (d/div
             {:className (css
                           :flex
                           :items-center
                           :py-2
                           ["& .decorator"
                            :text-transparent
                            {:transition "color .2s ease-in-out"
                             :position "absolute"
                             :right "0px"}]
                           ["&:hover .decorator" :text-gray-400])}
             ($ e/avatar
                {:className (css :mr-2
                                 :border
                                 :border-solid
                                 :border-gray-500)
                 :size :small
                 & value})
             (d/input
               {:ref input
                :className "input"
                :value search
                :read-only (or read-only (not searchable?))
                :disabled disabled
                :spellCheck false
                :auto-complete "off"
                :placeholder placeholder
                :onChange #(set-value! %)
                :onBlur sync-search!
                :onKeyDown on-key-down})
             (d/span
               {:className "decorator"}
               ($ icon/dropdownDecorator)))
           ($ dropdown/Popup
              {:className "dropdown-popup"
               :render/option e/identity-dropdown-option
               :render/wrapper e/dropdown-wrapper}))))))


; (defstyled action-cell table/ActionCell
;   {:padding 5
;    :font-size "0.8em"
;    :border-radius 3
;    :display "flex"
;    :justify-content "center"
;    :transition "box-shadow .3s ease-in,background .3s ease-in"
;    :border "2px solid transparent"
;    :align-items "center"
;    :cursor "pointer"
;    ":focus" {:outline "none"}}
;   )


(defnc delete-cell [])

; (defstyled delete-cell table/DeleteCell
;   {:display "flex"
;    :justify-content "center"
;    :align-content "center"
;    :min-height 25
;    :min-width 30
;    :max-height 30
;    :font-size "1em"
;    :align-items "center"
;    ; :margin-top 3
;    ".delete-marker"
;    {:cursor "pointer"
;     :margin "1px 3px"
;     :transition "color .3s ease-in"}}
;   )


(defnc expand-cell [])

; (defstyled expand-cell table/ExpandCell
;   {:display "flex"
;    :flex-grow "1"
;    :justify-content "center"
;    :cursor "pointer"
;    :svg {:transition "transform .3s ease-in-out"}}
;   )



;; Styled headers
(def $header
  (css
    :flex
    :text-sm
    :font-bold
    :flex-col
    :h-full
    :justify-between
    :m-1
    {:flex-grow "1"}
    ["& .header" :flex :row]
    ["& .header .row" :cursor-pointer :font-bold]
    ["& .sort-marker.hidden" {:opacity "0"}]))


(let [$default (css :justify-start :items-start)
      $top-center (css :justify-center :items-start)
      $top-right (css :justify-end :items-start)
      $center-left (css :justify-start :items-center)
      $center (css :justify-center :items-center)
      $center-right (css :justify-end :items-center)
      $bottom-left (css :justify-start :items-end)
      $bottom-center (css :justify-center :items-end)
      $bottom-right (css :justify-end :items-end)]

  (defhook use-cell-alignment-css
    "Pass in column and function will return css class for aligment"
    [{:keys [align]}]
    (case align
      (:center #{:top :center}) $top-center
      (:right #{:top :right}) $top-right
      #{:center :left} $center-left
      #{:center :right} $center-right
      #{:center} $center
      (:bottom  #{:bottom-left}) $bottom-left
      #{:bottom :center} $bottom-center
      #{:bottom :right} $bottom-right
      $default))
  (defhook use-header-alignment-css
    "Pass in column and function will return css class for aligment"
    [{:keys [align]}]
    (if (set? align)
      (condp some? align
        :center $top-center
        :right $top-right
        $default)
      (case align
        :center $top-center
        :right $top-right
        $default))))


(defnc plain-header
  [{:keys [className column] :as props}]
  (let [$alignment (use-header-alignment-css column)]
    (d/div
      {:class [$header className]}
      (d/div 
        {:class [$alignment "header"]}
        ($ table/SortElement {& props})
        ($ table/ColumnNameElement {& props})))))


(defnc identity-header [])


; (defstyled identity-header table/IdentityHeader
;   nil
;   #_(deep-merge
;     header-style
;     {".filter"
;      {:line-height 12
;       :padding 0
;       ; :flex-grow "1"
;       :justify-self "center"
;       :resize "none"
;       :border "none"
;       :width "100%"}}))


(defnc text-header
  [{{:keys [filter :filter/placeholder] :as column
     :or {placeholder "Filter..."}} :column
    :keys [className]
    :as props}]
  (let [v filter
        dispatch (table/use-dispatch)
        $alignment (use-header-alignment-css column)
        $filter (css
                  :p-0
                  :border-0
                  :w-full
                  :font-thin
                  :text-sm)]
    (d/div
      {:class [$header className]}
      (d/div
        {:class [$alignment "header"]}
        ($ table/SortElement {& props})
        ($ table/ColumnNameElement {& props}))
      (d/div
        {:className $filter}
        ($ e/idle-input
           {:placeholder placeholder
            :className "filter"
            :spellCheck false
            :auto-complete "off"
            :value (or v "")
            :onChange (fn [value]
                        (when dispatch
                          (dispatch
                            {:type :table.column/filter
                             :column column
                             :value (not-empty value)})))})))))

; (defstyled text-header table/TextHeader 
;   nil
;   #_(deep-merge
;     header-style
;     {".filter"
;      {:line-height 12
;       :padding 0
;       ; :flex-grow "1"
;       :justify-self "center"
;       :resize "none"
;       :border "none"
;       :width "100%"}}))



(defnc boolean-popup [])

; (defstyled boolean-popup table/BooleanFilter 
;   {:display "flex"
;    :flex-direction "row"
;    (str ui/checkbox) {:margin "1px 2px"}})


(defnc boolean-header [])

; (defstyled boolean-header table/BooleanHeader 
;   nil
;   #_(deep-merge 
;     header-style
;     {:align-items "center"}))



(defnc enum-popup [])

; (defstyled enum-popup popup/element
;   {(str ui/checklist " .name") {:font-size "1em"}})


(def popup-menu-preference
  [#{:bottom :center} 
   #{:left :center} 
   #{:right :center} 
   #{:top :center}])


(defnc header-popup-wrapper
  [{:keys [style] :as props} _ref]
  {:wrap [(ui/forward-ref)]}
  (let [$layout (css
                  :flex
                  :flex-col
                  :m-0
                  :p-2
                  :bg-gray-800
                  :shadow-xl
                  :rounded-xl
                  :border-2
                  :border
                  :border-gray-100
                  {:box-shadow "0 11px 25px -5px rgb(0 0 0 / 9%), 0 4px 20px 0px rgb(0 0 0 / 14%)"}
                  ; {:box-shadow "0px 3px 10px -3px black"}
                  ["& .simplebar-scrollbar:before"
                   :bg-gray-100
                   :pointer-events-none
                   {:max-height "400px"}])]
    (d/div
      {:ref _ref
       :class [$layout]
       :style style}
      (c/children props))))


(defnc enum-header
  [{:keys [className] :as props
    {:keys [filter options] :as column} :column }]
  (let [v filter
        [opened? set-opened!] (hooks/use-state nil)
        dispatch (table/use-dispatch) 
        area (hooks/use-ref nil)
        popup (hooks/use-ref nil)
        $alignment (use-header-alignment-css column)
        $filter (css
                  :flex
                  :flex-col
                  :items-center
                  ["& .filter" :flex :justify-center])]
    (popup/use-outside-action
      opened? area popup
      #(set-opened! false))
    (d/div
      {:class [$header $alignment className (css :flex-row)]}
      (d/div 
        {:class [$filter]}
        (d/div
          {:class ["header"]}
          ($ table/SortElement {& props})
          ($ table/ColumnNameElement {& props}))
        ($ popup/Area
           {:ref area
            :className "filter"
            :onClick (fn []
                       (set-opened! true)
                       #_(when opened? 
                           (.preventDefault e)))}
           ($ e/checkbox
              {:value (if (nil? v) nil (boolean (not-empty v)))})
           (when (and (not-empty options) opened?) 
             ($ popup/Element
                {:ref popup
                 :wrapper header-popup-wrapper
                 :preference popup-menu-preference}
                ($ e/checklist
                   {:value v
                    :multiselect? true
                    :options options 
                    :className (css
                                 ["& .row"
                                  :flex
                                  :items-center
                                  :cursor-pointer
                                  :mx-2
                                  :my-3
                                  :text-gray-500
                                  {:transition "all .2s ease-in-out"}]
                                 ["& .row .icon" :hidden]
                                 ["& .row.selected" :text-white]
                                 ["& .row.disabled" :pointer-events-none]
                                 ["& .row .name"
                                  :ml-2
                                  :select-none
                                  :text-sm
                                  :font-bold
                                  :uppercase])
                    :onChange #(dispatch
                                 {:type :table.column/filter
                                  :column column
                                  :value (when (not-empty %) (set %))})}))))))))


(defnc timestamp-header [])

; (defstyled timestamp-header table/TimestampHeader
;   nil
;   #_(deep-merge
;       header-style
;       {:justify-content "flex-start"
;        :align-items "center"
;        ; ".header" {:margin-left "-1em"}
;        }))


(defnc table
  [props]
  (let [[header {header-height :height}] (use-dimensions) 
        body (hooks/use-ref nil)
        {:keys [height width]} (layout/use-container-dimensions)
        [header-style body-style] (hooks/use-memo
                                    [height width header-height]
                                    [(cond->
                                       {:width width}
                                       header-height (assoc :height header-height))
                                     {:height (- height header-height)
                                      :width width}])
        scroll (hooks/use-ref nil)]
    (hooks/use-effect
      [@body @header]
      (letfn [(sync-body-scroll [e]
                (when-some [left (.. e -target -scrollLeft)]
                  (when (and @header (not= @scroll left)) 
                    (reset! scroll left)
                    (aset @header "scrollLeft" left))))
              (sync-header-scroll [e]
                (when-some [left (.. e -target -scrollLeft)]
                  (when (and @body (not= @scroll left)) 
                    (reset! scroll left)
                    (aset @body "scrollLeft" left))))]
        (when @body
          (when @header
            (.addEventListener @header "scroll" sync-header-scroll))
          (when @body
            (.addEventListener @body "scroll" sync-body-scroll))
          (fn [] 
            (when @body
              (.removeEventListener @body "scroll" sync-body-scroll))
            (when @header
              (.removeEventListener @header "scroll" sync-header-scroll))))))
    ($ table/TableProvider
       {& props}
       (d/div
         {:style {:display "flex"
                  :flex-direction "column"}}
         ($ layout/Container
            {:style header-style}
            ($ table/Header
               {:ref (fn [el] (reset! header el))
                :className (css :flex
                                :grow
                                :p-3
                                :border-1
                                :border-transparent
                                ["& .simplebar-scrollbar:before"
                                 {:visibility "hidden"}]
                                ["& .trow" :items-start])}))
         (when body-style
           ($ layout/Container
              {:style body-style}
              ($ table/Body
                 {:ref (fn [el] (reset! body el))
                  :className (css
                               :flex
                               :column
                               :grow
                               :p-3
                               :bg-gray-100
                               :border
                               :border-solid
                               :border-gray-300
                               :shadow-lg
                               :rounded-md)})))))))


(def components
  (merge
    {:table table}
    #:table {:row row}
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
    #:header {:plain plain-header
              :enum enum-header
              :boolean boolean-header
              :text text-header
              ; :identity identity-header
              :timestamp timestamp-header}))
