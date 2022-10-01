(ns toddler.elements.dropdown
  (:require
   clojure.string
   [helix.core :refer [defhook defnc create-context $ provider]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [toddler.hooks :refer [use-idle]]
   [toddler.elements.popup :as popup]
   [toddler.elements.input :refer [AutosizeInput]]
   ["toddler-icons$default" :as icon]))

(defn get-available-options
  ([search options search-fn]
   (let [options (distinct options)
         regex (when (not-empty search)
                 (re-pattern (apply str "(?i)" (clojure.string/replace search #"\s+" ".*"))))
         available-options (if regex
                             (let [predicate (comp (partial re-find regex) search-fn)]
                               (if (some (comp (partial = search) search-fn) options)
                                 options
                                 (filter predicate options)))
                             options)]
     (if (empty? available-options)
       (vec options)
       (vec available-options)))))

(defn next-option  [cursor [option :as options]]
  (let [cursor-position (inc (.indexOf options cursor))
        cursor' (get options cursor-position option)]
    cursor'))

(defn previous-option [cursor options]
  (let [cursor-position (dec (.indexOf options cursor))
        cursor' (get options cursor-position (last options))]
    cursor'))

(defn key-down-handler
  [e {:keys [value
             search
             opened
             cursor
             options
             new-fn
             position
             search-fn
             on-change
             set-opened!
             set-search!
             set-cursor!]
      :or {value []
           search-fn str}}]
  (case (.-keyCode e)
    ;; BACKSPACE
    8 (do
        (set-opened! true)
        (when (<= (count search) 1)
          (set-search! "")
          (when (fn? new-fn) (on-change nil))))
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
          (if (position :top)
            (next-option cursor options)
            (previous-option cursor options))))
    ;; KEY DOWN
    40 (do
         (.preventDefault e)
         (when-not opened
           (set-opened! true))
         (set-cursor!
          (if (position :top)
            (previous-option cursor options)
            (next-option cursor options))))
    ;; ALPHA-NUMERIC
    (48 49 50 51 52 53 54 55 56 57 65 66
        67 68 69 70 71 72 73 74 75 76 77 78
        79 80 81 82 83 84 85 86 87 88 89 90
        96 97 98 99 100 101 102 103 104 105) (when-not opened (set-opened! true))
    ;; EVERYTHING ELSE
    "default"))

(defn maybe-focus [input]
  (when @input (.focus @input)))

(defhook use-dropdown
  [{:keys [value options on-change onChange new-fn search-fn area area-position disabled]
    :or {search-fn str}
    :as props}]
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
     [value search-fn]
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
     :ref-fn ref-fn
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
                                       :position area-position
                                       :input @input}))
     :options available-options}))

(def ^:dynamic ^js *dropdown* (create-context))

(defnc Decorator
  [{:keys [className] :as props}]
  (let [{:keys [options opened disabled]} (hooks/use-context *dropdown*)]
    (when (and (not disabled) (pos? (count options)))
      (d/span
       {:className (str
                    className
                    (when opened " opened"))}
       (when icon/dropdownDecorator
         ($ icon/dropdownDecorator))))))


(defnc Discard
  [{:keys [className] :as props}]
  (let [{:keys [value discard!]} (hooks/use-context *dropdown*)]
    (when (some? value)
      (d/span
       {:className className
        :onClick discard!}
       (c/children props)))))


(defnc Area
  [props]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        ;;
        {:keys [area] :as dropdown}
        (use-dropdown
         (->
          props
          (assoc :area-position area-position)
          (dissoc :className)))]
    (provider
     {:context *dropdown*
      :value dropdown}
     (provider
      {:context popup/*area-position*
       :value [area-position set-area-position!]}
      ($ popup/Area
         {:ref area
          & (select-keys props [:className])}
         (c/children props))))))


(defnc Input
  [{:keys [className onSearchChange placeholder]
    rinput :render/input
    rwrapper :render/wrapper
    rimg :render/img
    :or {rwrapper "div"
         rinput AutosizeInput}
    :as props}]
  (let [{:keys [input
                value
                search
                on-change
                on-key-down
                sync-search!
                toggle!
                opened
                disabled
                read-only
                searchable?]
         :or {searchable? true}} (hooks/use-context *dropdown*)]
    (hooks/use-effect
      [search]
      (when (ifn? onSearchChange)
        (onSearchChange search)))
    ($ rwrapper
       {:onClick toggle!
        :className (cond-> className
                     opened (str " opened"))
        & (select-keys props [:context :disabled])}
       (when rimg ($ rimg {& value}))
       ($ rinput
          {:ref input
           :className "input"
           :value search
           :read-only (or read-only (not searchable?))
           :disabled disabled
           :spellCheck false
           :auto-complete "off"
           :placeholder placeholder
           :onChange on-change
           :onBlur sync-search!
           :onKeyDown on-key-down})
       (c/children props))))



(defnc Popup
  [{:keys [className]
    rpopup :render/popup
    roption :render/option}]
  (let [[area-position set-area-position!] (hooks/use-context popup/*area-position*)
        {:keys [options
                popup
                disabled
                opened
                read-only
                search-fn
                ref-fn
                cursor
                select!
                close!]
         :or {search-fn str}} (hooks/use-context *dropdown*)]
    (when (and (not read-only) (not disabled) (pos? (count options)) opened)
      ($ popup/Element
         {:ref popup
          :items options
          :onChange (fn [{:keys [position]}]
                      (when (some? position) (set-area-position! position)))
          :className (str className " animated fadeIn faster")
          :wrapper rpopup}
         (map
           (fn [option]
             ($ roption
               {:key (search-fn option)
                :ref (ref-fn option)
                :option option
                :selected (= option cursor)
                :onMouseDown (fn []
                               (select! option)
                               (close!))
                & (cond-> nil
                    (nil? option) (assoc :style #js {:color "transparent"}))}
               (if (nil? option) "nil" (search-fn option))))
           (if (:top area-position)
             (reverse options)
             options))))))


(defnc Element
  [{rinput :render/input
    rpopup :render/popup
    :as props}]
  ($ Area
    {& props}
    ($ rinput {& (dissoc props :render/input :render/popup)})
    ($ rpopup)))
