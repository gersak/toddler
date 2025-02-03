(ns toddler.dropdown
  (:require
   clojure.string
   [helix.core :refer [defhook defnc create-context $]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [toddler.core :refer [use-idle]]
   [toddler.popup :as popup]))

(defn ^:no-doc get-available-options
  "For given search string and options and search-fn
  result will return only options that are matched
  by search pattern"
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
                               (if (some (comp (partial = search) str search-fn) options)
                                 options
                                 ;; Otherwise return filtered options
                                 (filter predicate options)))
                             options)]
     (if (empty? available-options)
       (vec options)
       (vec available-options)))))

(defn ^:no-doc next-option
  [cursor [option :as options]]
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
             input popup]
      :or {value []
           search-fn str}}]
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
            (set-search! "")
            (if (fn? new-fn)
              (on-change (new-fn nil))
              (on-change nil))))
      ;; TAB
      9 (do
          (set-opened! false)
          (if (fn? new-fn)
            (on-change (new-fn search))
            (set-search! (search-fn value))))
      ;; ENTER
      13 (do
           (if (some? cursor)
             (on-change cursor)
             (when (fn? new-fn) (on-change (new-fn search))))
           (set-opened! false))
      ;; ESCAPE
      27 (do
           (set-opened! false)
           (set-search! (search-fn value)))
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

(defn ^:no-doc maybe-focus [input]
  (when @input (.focus @input)))

(defhook use-dropdown
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
   * ref-fn    - function that should be provided to option :ref. So that focus can be called
               to scroll into view selected option
   * read-only true|false
   * discard! - when called will reset dropdown
   * toggle!  - open|closes dropdown
   * open!    - opens dropdown
   * close!   - closes dropdown
   * select!  - will call on change for value
   * popup    - reference that should be passed to popup element
   * on-change   - Should be passed to input element
   * on-key-down - Should be passed to input element
   "
  [{:keys [value options on-change onChange
           new-fn search-fn area area-position disabled
           read-only context-fn]
    :or {search-fn str
         context-fn (constantly nil)}}]
  (let [on-change (or onChange on-change identity)
        [search set-search!] (use-idle
                              (search-fn value)
                               ;; This is where I receive new idle search value
                              (fn [v]
                                 ;; I wan't to check if there is new-fn so that
                                 ;; "New" valaue can be created
                                (when (fn? new-fn)
                                   ;; Than I wan't to call new-fn with new search input value
                                  (let [v' (new-fn (if (= v :NULL) nil v))]
                                     ;; and check if onChange should be called
                                    (when-not (= v' value)
                                      (on-change v'))))))
        [opened set-opened!] (hooks/use-state false)
        [cursor set-cursor!] (hooks/use-state value)
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
    (hooks/use-effect
      [value]
      ;; Search FN can change as well, but it is not expected to change search-fn
      ;; I.E. usually it will be keyword like :name, and ClojureScript generates
      ;; new :keyword object on every render, so this will reset set-search! that
      ;; was changed in on-change handler... This is a bug
      ;; [value search-fn]
      (when (not= (search-fn value) search)
        (set-search! (search-fn value))))
    (hooks/use-effect
      [opened]
      (when opened (focus value)))
    (popup/use-outside-action
     opened area popup
     #(set-opened! false))
    {:search search
     :value value
     :opened opened
     :focus set-cursor!
     :cursor cursor
     :input input
     :area area
     :search-fn search-fn
     :context-fn context-fn
     :ref-fn ref-fn
     :read-only read-only
     :discard! #(on-change nil)
     :sync-search! #(when-not (fn? new-fn) (set-search! (search-fn value)))
     :toggle! (fn [] (when-not disabled (maybe-focus input) (set-opened! not)))
     :open! (fn [] (maybe-focus input) (set-opened! true))
     :close! #(set-opened! false)
     :select! #(on-change %)
     :popup popup
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
                                       :popup @popup
                                       :input @input}))
     :options available-options}))

(def ^{:dynamic true
       :doc "Dropdown context. Used by Input, Options and Popup
            to apply logic from **use-dropdown** hook context"}
  ^js *dropdown* (create-context))

(defnc Input
  "Component will render input DOM element using 
  handlers and values from use-dropdown 
  hook provided by *dropdown* context."
  [{:keys [onSearchChange placeholder] :as props}]
  (let [{:keys [input
                search
                on-change
                on-key-down
                sync-search!
                disabled
                read-only
                searchable?]
         :or {searchable? true}} (hooks/use-context *dropdown*)]
    (hooks/use-effect
      [search]
      (when (ifn? onSearchChange)
        (onSearchChange search)))
    (d/input
     {:ref input
      :value (or search "")
      :read-only (or read-only (not searchable?))
      :disabled disabled
      :spellCheck false
      :auto-complete "off"
      :placeholder placeholder
      :onChange on-change
      :onBlur sync-search!
      :onKeyDown on-key-down
      & (select-keys props [:className :class])})))

(defnc Options
  "Component will render options DOM elements using 
  handlers and values from use-dropdown 
  hook provided by *dropdown* context."
  [{:keys [render]}]
  (let [{:keys [options
                search-fn
                context-fn
                ref-fn
                cursor
                select!
                close!]
         :or {search-fn str}} (hooks/use-context *dropdown*)
        popup-position (hooks/use-context popup/*position*)]
    (map
     (fn [option]
       ($ render
          {:key (search-fn option)
           :ref (ref-fn option)
           :value option
           :context (when (ifn? context-fn)
                      (context-fn option))
           :selected (= option cursor)
           :onMouseDown (fn []
                          (select! option)
                          (close!))
           & (cond-> nil
               (nil? option) (assoc :style #js {:color "transparent"}))}
          (if (nil? option) "nil" (search-fn option))))
     (if (:top popup-position)
       (reverse options)
       options))))

(defnc Popup
  "Component will render popup element using 
  handlers and values from use-dropdown 
  hook provided by *dropdown* context."
  [{:keys [preference]
    :or {preference [#{:bottom :left} #{:top :left}]}
    :as props}]
  (let [{:keys [options
                popup
                disabled
                opened
                read-only]} (hooks/use-context *dropdown*)]
    (when (and (not read-only) (not disabled) (pos? (count options)) opened)
      ($ popup/Element
         {:ref popup
          :preference preference
          & (select-keys props [:style :className :class])}
         (c/children props)))))
