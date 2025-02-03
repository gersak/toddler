;; Copyright (C) Neyho, Inc - All Rights Reserved
;; Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <robi@neyho.com>, June 2019


(ns toddler.multiselect
  (:require
   [helix.core :refer [$ defhook defnc provider <>]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   clojure.string
   [toddler.dropdown
    :refer [*dropdown*]]
   [toddler.popup
    :as popup]
   [toddler.ui :as ui :refer [!]]))

(defn ^:no-doc get-available-options
  ([search options search-fn]
   (let [options (distinct options)
         regex (when (not-empty search)
                 (re-pattern (apply str "(?i)" (clojure.string/replace search #"\s+" ".*"))))
         available-options (if regex
                             (let [predicate (fn [v]
                                               (when-let [text (search-fn v)]
                                                 (re-find regex text)))]
                               ;; Check if current search-fn matches some option
                               ;; and if it does return all options
                               (if (some (comp #(= search %) search-fn) options)
                                 options
                                 ;; Otherwise return filtered options
                                 (filter predicate options)))
                             options)]
     (if (empty? available-options)
       (vec options)
       (vec available-options)))))

(defn ^:no-doc next-option  [cursor [option :as options]]
  (let [cursor-position (inc (.indexOf options cursor))
        cursor' (get options cursor-position option)]
    cursor'))

(defn ^:no-doc previous-option [cursor options]
  (let [cursor-position (dec (.indexOf options cursor))
        cursor' (get options cursor-position (last options))]
    cursor'))

(defn ^:no-doc key-down-handler
  [e {:keys [value
             search
             opened
             cursor
             options
             new-fn
             search-fn
             on-change
             set-opened!
             set-search!
             set-cursor!
             input
             popup]
      :or {value []}}]
  (let [input-position (when input (.-top (.getBoundingClientRect input)))
        popup-position (when popup (.-top (.getBoundingClientRect popup)))
        position (if (<= popup-position input-position)
                   :bottom
                   :top)]
    (case (.-keyCode e)
      ;; BACKSPACE
      8 (do
          (set-opened! true)
          (when (<= (count search) 1)
            (when (= search "") (on-change (vec (butlast value))))
            (set-search! "")))
      ;; TAB
      9 (do
          (set-opened! false)
          (if (fn? new-fn)
            (on-change (new-fn search))
            (set-search! "")))
      ;; ENTER
      13 (do
           (if cursor
             (on-change ((fnil conj []) value cursor))
             (let [v (.. e -target -value)
                   option (some
                           (fn [option]
                             (when (= (search-fn option) v) option))
                           options)]
               (cond
                 (some? option) (on-change ((fnil conj []) value option))
                 (fn? new-fn) (on-change ((fnil conj []) value (new-fn v)))
                 :else nil)
               (set-search! "")))
           (set-cursor! nil)
           (set-opened! false))
      ;; ESCAPE
      27 (do
           (set-opened! false)
           (set-cursor! nil))
      ;; KEY UP
      38 (do
           (when-not opened
             (set-opened! true))
           (.preventDefault e)
           (set-cursor!
            (case position
              :bottom (next-option cursor options)
              :top (previous-option cursor options))))
      ;; KEY DOWN
      40 (do
           (.preventDefault e)
           (when-not opened
             (set-opened! true))
           (set-cursor!
            (case position
              :top (next-option cursor options)
              :bottom (previous-option cursor options))))
      ;; ALPHA-NUMERIC
      (48 49 50 51 52 53 54 55 56 57 65 66
          67 68 69 70 71 72 73 74 75 76 77 78
          79 80 81 82 83 84 85 86 87 88 89 90
          96 97 98 99 100 101 102 103 104 105) (when-not opened (set-opened! true))
      ;; EVERYTHING ELSE
      "default")))

(defhook use-multiselect
  "Abstracts dropdown mechanincs. Expects options prop
  that holds available options for dropdown, value and
  on-change callback that will be called with actual value
  of selected option.

  Optional:

   * search-fn - will be applied to options to display string representation
   * new-fn    - when provided will be called when search-fn doesn't match
                 any of available options
  
  Hook will return map
  with following state and handler keys:

   * search  - value that should be displayed in input
   * value   - actual value
   * opened  - true|false if dropdown is opened
   * options - dropdown options
   * focus   - when called will focus option
   * cursor  - position of selected value
   * input   - reference that you should add to input element
   * area    - reference that you should add to dropdown element
   * search-fn - function that will be used to compute how to display options. Should return string
   * read-only true|false
   * discard! - when called will reset dropdown
   * toggle!  - open|closes dropdown
   * open!    - opens dropdown
   * close!   - closes dropdown
   * select!  - will call on change for value
   * popup    - reference that should be passed to popup element
   * on-change   - Should be passed to input element
   * on-key-down - Should be passed to input element"
  [{:keys [value options on-change onChange
           new-fn search-fn context-fn area
           area-position read-only]
    :or {search-fn str
         context-fn (constantly nil)}
    :as props}]
  (let [on-change (or onChange on-change identity)
        [search set-search!] (hooks/use-state nil)
        ; [search set-search!] (use-idle 
        ;                        ; (search-fn value)
        ;                        nil
        ;                        (fn [v] 
        ;                          (when (fn? new-fn) 
        ;                            (let [v' (new-fn (if (= v :NULL) nil v))]
        ;                              (when-not (= v' value)
        ;                                (on-change v'))))))
        [opened set-opened!] (hooks/use-state false)
        [cursor set-cursor!] (hooks/use-state nil)
        input (hooks/use-ref nil)
        _area (hooks/use-ref nil)
        area (or area _area)
        popup (hooks/use-ref nil)
        direction (condp #(contains? %2 %1) area-position
                    :top true
                    :center true
                    false)
        available-options (get-available-options search options search-fn)
        [ref-fn focus] (popup/use-focusable-items direction)]
    (hooks/use-effect
      [search]
      (when-let [o (some #(when (= search (search-fn %)) %) options)]
        (focus o)
        (set-cursor! o)))
    ; (hooks/use-effect
    ;   [value]
    ;   (when (not= (search-fn value) search) 
    ;     (set-search! (search-fn value))))
    (hooks/use-effect
      [opened]
      (when opened (focus value)))
    (popup/use-outside-action
     opened area popup
     #(when opened
        (set-opened! false)))
    (assoc props
      :search search
      :opened opened
      :cursor cursor
      :input input
      :area area
      :popup popup
      :context-fn context-fn
      :ref-fn ref-fn
      :read-only read-only
      :toggle! (fn []
                 (when @input (.focus @input))
                 (set-opened! not))
      :open! (fn []
               (when @input (.focus @input))
               (set-opened! true))
      :close! #(set-opened! false)
      :select! #(let [current ((fnil conj []) value %)]
                  (on-change current)
                  #_(set-search! "")
                  #_(.focus @input))
      :remove! #(do
                  (on-change
                   (when-let [v (not-empty (remove #{%} value))]
                     (vec v)))
                  #_(.focus @input))
      :on-change (fn [e] (set-search! (.. e -target -value)))
      :on-key-down (fn [e]
                     (key-down-handler e
                                       {:value value
                                        :search search
                                        :opened opened
                                        :cursor cursor
                                        :options available-options
                                        :new-fn new-fn
                                        :search-fn search-fn
                                        :on-change on-change
                                        :set-opened! set-opened!
                                        :set-search! set-search!
                                        :set-cursor! set-cursor!
                                        :position area-position
                                        :input @input
                                        :popup @popup}))
      :options available-options)))

(defnc Option
  "Default option element"
  [{:keys [value]
    :as props}]
  (d/div
   {& props}
   (if-some [children (c/children props)]
     children
     value)))

(defnc Options
  "Component will use options from *dropdown* context to
  render data from context using :render prop.
  
  ignore-select will stop propagation of mouse-down event. Should
  be true if you don't want popup to close, or false if you wan't
  to close popup on multiselect option select."
  [{:keys [render context-fn ignore-select]
    :or {ignore-select true
         render Option}}]
  (let [{:keys [options
                search-fn
                ref-fn
                value
                remove!
                select!]
         :or {search-fn str}} (hooks/use-context *dropdown*)
        popup-position (hooks/use-context popup/*position*)
        is-selected? (hooks/use-memo
                       [value]
                       (set value))
        not-selected (remove is-selected? options)]
    (map
     (fn [option]
       (let [selected (is-selected? option)]
         ($ render
            {:key (search-fn option)
             :ref (ref-fn option)
             :value option
             :context (when (ifn? context-fn)
                        (context-fn option))
             :selected selected
             :onMouseDown (fn [e]
                            (when ignore-select (.stopPropagation e))
                            ((if selected remove! select!)
                             option))
             & (cond-> nil
                 (nil? option) (assoc :style #js {:color "transparent"}))}
            (if (nil? option) "nil" (search-fn option)))))
     (if (:top popup-position)
       (reverse not-selected)
       not-selected))))
