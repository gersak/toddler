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
    [toddler.ui :as ui]
    ["toddler-icons$default" :as icon]))

(defn get-available-options 
  ([search value options search-fn]
   (let [options (remove (set value) (distinct options))
         regex (when (not-empty search)
                 (re-pattern (apply str "(?i)" (clojure.string/replace search #"\s+" ".*"))))
         available-options (if regex
                             (let [predicate (comp (partial re-find regex) search-fn)]
                               (if (some (comp #(= search %) search-fn) options)
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

(defn key-down-handler [e {:keys [value
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
                           :or {value []}}]
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


(defhook use-multiselect
  [{:keys [value options on-change onChange new-fn search-fn area area-position]
    :or {search-fn str}
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
        available-options (get-available-options search value options search-fn)
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
      #(set-opened! false))
    (assoc props
           :search search
           :opened opened
           :cursor cursor
           :input input
           :area area
           :popup popup
           :ref-fn ref-fn
           :toggle! (fn []
                      (when @input (.focus @input))
                      (set-opened! not))
           :open! (fn []
                    (when @input (.focus @input))
                    (set-opened! true))
           :close! #(set-opened! false)
           :select! #(do 
                       (on-change ((fnil conj []) value %))
                       (set-search! "")
                       (set-opened! true)
                       (.focus @input))
           :remove! #(do
                       (on-change (when-let [v (not-empty (remove #{%} value))]
                                    (vec v)))
                       (set-opened! false))
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
           :options available-options)))


(defnc Element
  [{:keys [context-fn search-fn disabled placeholder]
    :or {search-fn str}
    :as props}]
  (let [[area-position set-area-position!] (hooks/use-state nil)
        {:keys [open!
                remove!
                options
                new-fn
                area]
         :as multiselect} (use-multiselect
                            (assoc props
                                   :search-fn search-fn
                                   :area-position area-position))]
    (provider
      {:context *dropdown*
       :value multiselect}
      (provider
        {:context popup/*area-position*
         :value [area-position set-area-position!]}
        (<>
          (map
            (fn [option]
              ($ ui/option
                {:key (search-fn option)
                 :value option
                 :onRemove #(remove! option)
                 :context (if disabled :stale
                            (when (fn? context-fn)
                              (context-fn option)))}))
            (:value props))
          ($ popup/Area
             {:ref area
              :onClick #(when-not (empty? options) (open!))
              :className "dropdown"}
             (when (or (fn? new-fn) (not-empty options))
               ($ ui/input {:placeholder placeholder}))
             ($ ui/popup)))))))


(defnc Option
  [{:keys [value
           context
           on-remove
           onRemove
           disabled
           className]
    :as props}]
  (let [on-remove (some #(when (fn? %) %) [onRemove on-remove])]
    (d/div
      {:context (if disabled :stale context)
       :className className}
      (if-some [children (c/children props)]
        children
        value)
      (when on-remove
        ($ icon/clear
           {:className "remove"
            :onClick (fn [e]
                       (.stopPropagation e)
                       (on-remove value))})))))
