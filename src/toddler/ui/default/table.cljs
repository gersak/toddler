(ns toddler.ui.default.table
  (:require
    goog.string
    [clojure.string :as str]
    [helix.dom :as d]
    [helix.core 
     :refer [defnc $ provider]]
    [helix.hooks :as hooks]
    [helix.placenta.util
     :refer [deep-merge]]
    [helix.styled-components
     :refer [defstyled]]
    [shadow.css :refer [css]]
    ; [toddler.elements :as toddler]
    [toddler.hooks :refer [use-delayed use-translate]]
    [toddler.table :as table]
    [toddler.popup :as popup]
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
                       {:min-height "42px"})]))

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
        $button (css :w-10 :p-2
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
                   :shadow-md
                   :font-bold
                   {:background-color "#424242d1"})
        $copied (css
                  :text-green-300)]
    ($ popup/Area
       {:ref area
        :className (css
                     :py-1)
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
         (println "COPIED" copied?)
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
  (let [{:keys [placeholder options] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        [options' om vm] 
        (hooks/use-memo
          [options]
          (let [options' (range (count options))]
            [options'
             (reduce
               (fn [r idx]
                 (assoc r idx (get options idx)))
               nil
               options')
             (reduce
               (fn [r idx]
                 (assoc r (get-in options [idx :value]) idx))
               nil
               options')]))
        ;;
        [area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [input
                search
                on-key-down
                sync-search!
                disabled
                toggle!
                area]
         :as dropdown}
        (dropdown/use-dropdown (assoc column
                                      :value (get vm value)
                                      :searchable? false
                                      :area-position area-position
                                      :search-fn #(get-in om [% :name])
                                      :options options'
                                      :onChange #(set-value! (get-in om [% :value]))))]
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
             (d/input
               {:ref input
                :className "input"
                :value search 
                :read-only true
                :disabled disabled
                :spellCheck false
                :auto-complete "off"
                :placeholder placeholder
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
  (let [{:keys [placeholder options read-only] 
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
        :placeholder placeholder})))



(defnc timestamp-cell
  []
  nil)


;(defstyled timestamp-cell table/TimestampCell
;  {:display "flex"
;   :justify-content "center"
;   :input
;   {:font-size "1em"
;    :border "none"
;    :outline "none"
;    :resize "none"
;    :padding 2
;    :width "100%"}}
;  ;;
;  )



(defnc integer-cell
  []
  (let [{:keys [placeholder read-only disabled] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        translate (use-translate)
        [focused? set-focused!] (hooks/use-state false)
        $input (css
                 :outline-none
                 :border-0
                 :text-sm
                 :w-full
                 :py-2)]
    (d/input
      {:value (if value
                (if focused? 
                  (str value) 
                  (translate value))
                "")
       :class [$input]
       :placeholder placeholder
       :read-only read-only
       :disabled disabled
       :onFocus #(set-focused! true)
       :onBlur #(set-focused! false)
       :onChange (fn [e] 
                   (let [number (.. e -target -value)]
                     (when-some [value (try
                                         (js/parseInt number)
                                         (catch js/Error _ nil))]
                       (set-value! value))))})))


(defnc float-cell
  []
  (let [{:keys [placeholder read-only disabled] :as column} (table/use-column)
        [value set-value!] (table/use-cell-state column)
        translate (use-translate)
        [focused? set-focused!] (hooks/use-state false)
        $float (css
                 :border-0
                 :outline-0
                 :text-sm
                 :py-2)]
    ($ ui/autosize-input
       {:className $float
        :value (if value
                 (if focused? 
                   (str value) 
                   (translate value))
                 "")
        :placeholder placeholder
        :read-only read-only
        :disabled disabled
        :onFocus #(set-focused! true)
        :onBlur #(set-focused! false)
        :onChange (fn [e] 
                    (let [number (.. e -target -value)]
                      (when-some [value (try
                                          (js/parseFloat number)
                                          (catch js/Error _ nil))]
                        (set-value! value))))})))



(defstyled currency-cell table/CurrencyCell
  {:font-size "1em"
   :max-width 140
   :display "flex"
   :align-items "center"
   :input {:outline "none"
           :border "none"
           :max-width 100}}
  )


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
    #:table {:row row
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
            :identity identity-cell
            }
    #:header {:enum enum-header
              :boolean boolean-header
              :text text-header
              ; :identity identity-header
              :timestamp timestamp-header}))
