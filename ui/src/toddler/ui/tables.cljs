(ns toddler.ui.tables
  (:require
   goog.string
   [clojure.string :as str]
   [helix.dom :as d]
   [helix.core
    :refer [defnc $ provider]]
   [helix.hooks :as hooks]
   [helix.children :as c]
   [shadow.css :refer [css]]
   [toddler.core :refer [conj-prop-classes
                         use-delayed
                         use-translate
                         use-dimensions]]
   [toddler.ui.fields :refer [$dropdown-popup]]
   [toddler.material.outlined :as outlined]
   [toddler.fav6.solid :as solid]
   [toddler.table :as table]
   [toddler.popup :as popup]
   [toddler.layout :as layout]
   [toddler.dropdown :as dropdown]
   [toddler.input :refer [TextAreaElement AutosizeInput]]
   [toddler.ui.elements :as e]
   [toddler.ui :as ui]
   [toddler.i18n :as i18n]))

(defnc NotImplemented
  []
  (let [field (table/use-column)]
    (.error js/console (str "Field not implemented\n%s" (pr-str field))))
  nil)

(defnc row
  {:wrap [(ui/forward-ref)]}
  [props _ref]
  ($ table/Row
     {:ref _ref
      :className "trow"
      & (dissoc props :className :class)}))

(defnc cell
  {:wrap [(ui/forward-ref)]}
  [props _ref]
  ($ table/Cell
     {:ref _ref
      :className "tcell"
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
                     :color--
                     :rounded-sm
                     :flex
                     :justify-center
                     :items-center
                     ["&:hover" :color])
        $popup (css
                :shadow-md)
        $tooltip (css
                  :flex
                  :text-xxs
                  :justify-start
                  :wrap
                  :p-1
                  :font-semibold
                  :shadow-lg
                  {:border-radius "3px"
                   :background-color "var(--tooltip-bg)"
                   :color "var(--tooltip-color)"})
        $copied (css
                 {:color "var(--button-positive-hover-color)"})]
    ($ popup/Area
       {:ref area
        :className (css {:padding-top "0.45em"})
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
        ($ solid/barcode))
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
          (assoc column :value value)))]
    (provider
     {:context dropdown/*dropdown*
      :value dropdown}
     ($ popup/Area
        {:ref area
         :onClick toggle!
         :className (str/join " "
                              [(css :text-xs)])}
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
           :value (or search "")
           :disabled disabled
           :spellCheck false
           :auto-complete "off"
           :placeholder (or placeholder label)
           :onChange on-change
           :onBlur sync-search!
           :onKeyDown on-key-down}))
        ($ dropdown/Popup
           {:class ["dropdown-popup" $dropdown-popup]}
           ($ e/dropdown-wrapper
              ($ dropdown/Options
                 {:render e/dropdown-option})))))))

(defnc text-cell
  []
  (let [{:keys [label placeholder options read-only]
         {:keys [width]} :style
         :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        $text (css
               :text-xs
               :outline-none
               :py-2
               :w-full
               {:resize "none"})]
    ($ TextAreaElement
       {:value value
        :class [$text]
        :read-only read-only
        :spellCheck false
        :auto-complete "off"
        :style {:maxWidth width}
        :onChange set-value!
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
        popup (hooks/use-ref nil)
        calendar (ui/use-component :calendar)]
    (popup/use-outside-action
     opened area popup
     #(set-opened! false))
    ($ popup/Area
       {:ref area
        :onClick (fn []
                   (when (and (not disabled) (not read-only))
                     (set-opened! true)))
        :className (str/join " "
                             [(css
                               :py-2
                               :grow
                               :flex
                               :text-xs
                               ["& .clear"
                                :self-center
                                :text-transparent
                                {:transition "color .2s ease-in-out"
                                 :position "absolute"
                                 :right "0px"}]
                               ["&:hover .clear " :text-gray-400]
                               ["& .clear:hover" :text-gray-900 :cursor-pointer])])}
       (d/input
        {:className (css :text-xs)
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
          #_($ icon/clear)))
       (when opened
         ($ popup/Element
            {:ref popup
             :class ["dropdown-popup" $dropdown-popup]}
            ($ e/dropdown-wrapper
               {:max-height "30rem"}
               ($ calendar
                  {:value value
                   :on-change set-value!})))))))

(letfn [(text->number [text]
          (cond
            (number? text) text
            (empty? text) nil
            :else
            (let [number (js/parseInt  text)]
              (when-not (js/Number.isNaN number) (int number)))))]
  (defnc integer-cell
    []
    (let [{:keys [placeholder read-only disabled label element/width] :as column} (table/use-column)
          [value set-value!] (table/use-cell-state column)
          translate (use-translate)
          [focused? set-focused!] (hooks/use-state false)
          [input set-input!] (hooks/use-state (when value (translate value)))
          $input (css
                  :outline-none
                  :border-0
                  :text-xs
                  :w-full
                  :py-2)]
      (hooks/use-effect
        [focused?]
        (set-input! (str value)))
      (hooks/use-effect
        [value]
        (when-not (= (text->number input) value)
          (set-input! (when value (translate value)))))
      (d/div
       (d/input
        {:value (if value
                  (if focused?
                    input
                    (translate value))
                  "")
         :style (when width {:max-width width})
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
                             (set-value! number)))))})))))

(letfn [(text->number [text]
          (cond
            (number? text) text
            (empty? text) nil
            :else
            (let [number (js/parseFloat text)]
              (when-not (js/Number.isNaN number) (float number)))))]
  (defnc float-cell
    []
    (let [{:keys [placeholder read-only disabled label element/width] :as column} (table/use-column)
          [value set-value!] (table/use-cell-state column)
          translate (use-translate)
          [input set-input!] (hooks/use-state (when value (translate value)))
          [focused? set-focused!] (hooks/use-state false)
          $float (css
                  :flex
                  ["& input"
                   :border-0
                   :outline-0
                   :text-xs
                   :py-2
                   {:flex "1 0 auto"}])]
      (hooks/use-effect
        [focused?]
        (set-input! (str value)))
      (hooks/use-effect
        [value]
        (when-not (= (text->number input) value)
          (set-input! (when value (translate value)))))
      (d/div
       {:className $float}
       (d/input
        {:value (if value
                  (if focused?
                    input
                    (translate value))
                  "")
         :placeholder (or placeholder label)
         :style (when width {:max-width width})
         :read-only read-only
         :disabled disabled
         :onFocus #(set-focused! true)
         :onBlur #(set-focused! false)
         :onChange (fn [e]
                     (let [text (as-> (.. e -target -value) t
                                  (re-find #"[\d\.,]*" t)
                                  (str/replace t #"\.(?=[^.]*\.)" "")
                                  (str/replace t #"[\.\,]+" "."))
                           number (js/parseFloat text)
                           dot? (#{\.} (last text))]
                       (if (empty? text) (set-value! nil)
                           (when-not (js/Number.isNaN number)
                             (when (or (not= number value) dot?)
                               (set-input! text))
                             (when-not dot? (set-value! number))))))})))))

(defnc currency-cell
  []
  (let [{:keys [disabled placeholder label
                onFocus on-focus onBlur on-blur
                read-only]
         :as column} (table/use-column)
        [{:keys [currency amount] :as value} set-value!]
        (table/use-cell-state column)
        ;;
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [input area toggle!]}
        (dropdown/use-dropdown
         (->
          (hash-map
           :value currency
           :area-position area-position
           :options ["EUR" "USD" "JPY" "AUD" "CAD" "CHF"]
           :onChange #(set-value! (assoc value :currency %)))
          (dissoc :className)))
        on-blur (or onBlur on-blur identity)
        on-focus (or onFocus on-focus identity)
        [focused? set-focused!] (hooks/use-state nil)
        [state set-state!] (hooks/use-state (when amount (i18n/translate amount currency)))]
    (hooks/use-effect
      [value]
      (if focused?
        (set-state! (str amount))
        (if (and amount currency)
          (i18n/translate amount currency)
          (set-state! ""))))
    (d/div
     {:class (css
              :py-2
              :flex
              :grow
              :text-xs
              ["& .clear"
               :text-md
               :self-center
               :text-transparent
               {:transition "color .2s ease-in-out"
                :position "absolute"
                :right "0px"}]
              ["&:hover .clear " :text-gray-400]
              ["& .clear:hover" :text-gray-900 :cursor-pointer])
      :onClick (fn []
                 (when (and currency @input)
                   (set-state! (str amount))
                   (.focus @input)))}
     ($ popup/Area
        {:ref area}
        (d/input
         {:value (or currency "")
          :className (css
                      :w-10
                      :font-bold
                      :cursor-pointer)
          :read-only true
          :onClick toggle!
          :placeholder "VAL"})
        ($ dropdown/Popup
           {:render/option e/dropdown-option
            :render/wrapper e/dropdown-wrapper}))
     (d/input
      {:ref input
       :value state
       :placeholder (or placeholder label)
       :read-only read-only
       :disabled (or disabled (not currency))
       :onFocus (fn [e]
                  (set-focused! true)
                  (when (and currency amount)
                    (set-state! (i18n/translate amount currency)))
                  (on-focus e))
       :onBlur (fn [e]
                 (set-focused! false)
                 (when amount
                   (set-state! (i18n/translate amount currency)))
                 (on-blur e))
       :onChange (fn [e]
                   (when (some? currency)
                     (let [text (as-> (.. e -target -value) t
                                  (re-find #"[\d\.,]*" t)
                                  (str/replace t #"\.(?=[^.]*\.)" "")
                                  (str/replace t #"[\.\,]+" "."))
                           number (js/parseFloat text)
                           dot? (#{\.} (last text))]
                       (if (empty? text)
                         (set-value! {:amount nil
                                      :currency currency})
                         (when-not (js/Number.isNaN number)
                           (when (or (not= number value) dot?)
                             (set-state! text))
                           (when-not dot?
                             (set-value!
                              {:amount number
                               :currency currency})))))))})
     (when value
       (d/span
        {:class (cond-> ["clear"]
                  focused? (conj "opened"))
         :onClick (fn [e]
                    (.stopPropagation e)
                    (.preventDefault e)
                    (set-value! nil)
                    (set-focused! false))}
        #_($ icon/clear))))))

(defnc boolean-cell
  []
  (let [{:keys [read-only disabled] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        $button (css
                 :text-xs
                 :w-5
                 :h-5
                 :flex
                 :rounded-sm
                 :justify-center
                 :items-center
                 {:transition "color .3s ease-in-out"})
        $active (css :color++)
        $inactive (css  :color--)]
    (d/div
     {:class [(css
               :py-2)]}
     (d/button
      {:disabled disabled
       :read-only read-only
       :class (cond->
               [$button]
                value (conj $active)
                (not value) (conj $inactive))
       :onClick #(set-value! (not value))}
      ($ (case value
           nil outlined/check-box-outline-blank
           outlined/check)
         {:className (css :w-6 :h-6)})))))

(defnc identity-cell
  []
  (let [{:keys [placeholder] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        ;;
        {:keys [input
                search
                on-change
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
                                 :on-change set-value!
                                 :search-fn :name))]
    (provider
     {:context dropdown/*dropdown*
      :value dropdown}
     ($ popup/Area
        {:ref area
         :onClick toggle!
         :className (str/join
                     " "
                     [(css :text-xs :pl-10)])}
        (d/div
         {:className (css
                      :flex
                      :items-center
                      :py-2
                      {:max-height "40px"
                       :position "relative"}
                      ["& .decorator"
                       :text-transparent
                       {:transition "color .2s ease-in-out"
                        :position "absolute"
                        :right "0px"}]
                      ["&:hover .decorator" :text-gray-400])}
         ($ ui/avatar
            {:className (css
                         :border
                         :border-solid
                         :border-gray-500
                         :w-6 :h-6
                         :absolute
                         {:border-radius "30px"
                          :left "-32px"
                          :top "50%"
                          :background-color "var(--avatar-bg)"
                          :transform "translateY(-50%)"})
             :size :small
             & value})
         ($ AutosizeInput
            {:ref input
             :value (or search "")
             :read-only (or read-only (not searchable?))
             :disabled disabled
             :spellCheck false
             :auto-complete "off"
             :placeholder placeholder
             :onChange on-change
             :onBlur sync-search!
             :onKeyDown on-key-down}))
        ($ dropdown/Popup
           {:class ["dropdown-popup" $dropdown-popup]}
           ($ e/dropdown-wrapper
              ($ dropdown/Options
                 {:render e/identity-dropdown-option})))))))

(defnc delete-cell [])

(defnc expand-cell
  []
  (let [column (table/use-column)
        [value set-value!] (table/use-cell-state column)
        $expand (css
                 :text-xs
                 :outline-none
                 :w-full
                 :flex
                 :justify-center
                 {:resize "none"
                  :padding-top "0.7em"})]
    (d/div
     {:class [$expand]}
     (if value
       ($ solid/arrow-right {:onClick #(set-value! (not value))})
       ($ solid/arrow-down {:onClick #(set-value! (not value))})))))

;; Styled headers
(def $header
  (css
   :flex
   :text-xs
   :font-bold
   :flex-col
   :h-full
   :justify-between
   :m-1
   :grow
   ["& .header" :flex :row]
   ["& .header .row" :cursor-pointer :font-bold]
   ["& .sort-marker.hidden" {:opacity "0"}]))

(defnc SortElement
  [{{:keys [order]} :column}]
  #_(case order
      :desc
      ($ outlined/arrow-drop-down
         {:className "sort-marker"})
      :asc
      ($ outlined/arrow-drop-up
         {:className "sort-marker"})
    ;;
      ($ outlined/arrow-drop-down
         {:className "sort-marker hidden"})))

(defnc plain-header
  [{:keys [className] :as props}]
  (d/div
   {:class [$header className]}
   (d/div
    {:class ["header"]}
    ($ SortElement {& props})
    ($ table/ColumnNameElement {& props}))))

(def popup-menu-preference
  [#{:bottom :center}
   #{:left :center}
   #{:right :center}
   #{:top :center}])

(defnc header-popup-wrapper
  {:wrap [(ui/forward-ref)]}
  [{:keys [style] :as props} _ref]
  (let [$layout (css
                 :flex
                 :flex-col
                 :m-0
                 :p-2
                 :bg-gray-100
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

(def $table
  (css
   :flex
   :column
   :grow
   :text-neutral-600
   :border
   :border-solid
   :rounded-md

   {:background-color "var(--table-bg)"
    :border-color "var(--table-border)"
    :color "var(--table-text)"}
   ["&:not([flavor=\"flat\"])" :shadow-lg]
   ["& .tcell" :overflow-hidden]
   ["& .trow"
    :my-1
    :border-b
    :border-transparent
    {:min-height "2em"
     :transition "all .5s ease-in-out"}]
   ["& .trow:hover, & .trow:focus-within" :border-b
    {:border-color "var(--table-hover-border)"
     :background-color "var(--table-hover-bg)"}]))

(defnc table
  [props]
  (let [{:keys [height width]} (layout/use-container-dimensions)
        body (hooks/use-ref nil)
        ;;
        [header {header-height :height}] (use-dimensions)
        ;;
        [header-style body-style]
        (hooks/use-memo
          [height width header-height]
          [(cond->
            {:width width}
             header-height (assoc :height header-height))
           {:height (- height header-height)
            :width width}])
        ;;
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
                 :flex-direction "column"}
         :class (conj-prop-classes props)}
        (provider
         {:context layout/*container-dimensions*
          :value header-style}
         (let [$style (css :flex
                           :p-3
                           :border-1
                           :border-transparent
                           ["& .simplebar-scrollbar:before"
                            {:visibility "hidden"}]
                           ["& .trow" :items-start :flex])]
           ($ table/Header
              {:ref (fn [el] (reset! header el))
               :className $style})))
        (when body-style
          (provider
           {:context layout/*container-dimensions*
            :value body-style}
           ($ table/Body
              {:ref (fn [el] (reset! body el))
               :flavor (:flavor props)
               :className $table})))))))

(def components
  (merge
   {:table table}
   #:table {:row row
            :cell cell}
   #:cell {:expand expand-cell
           :delete delete-cell
           :boolean boolean-cell
           :currency currency-cell
           :enum enum-cell
           :float float-cell
            ; :hashed table/HashedCell
           :integer integer-cell
           :uuid uuid-cell
           :text text-cell
           :timestamp timestamp-cell
           :identity identity-cell}
   #:header {:plain plain-header}))
