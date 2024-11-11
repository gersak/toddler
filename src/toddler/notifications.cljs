(ns toddler.notifications
  (:require
    clojure.string
    goog.string.format
    shadow.css
    [clojure.core.async :as async]
    [helix.core :refer [defnc $ <> memo]]
    [helix.children :refer [children]]
    [toddler.hooks :refer [use-toddler-listener]]
    [helix.hooks :as hooks]
    [helix.dom :as d]))


(defonce notification-channel (async/chan 1000))


(defn add-notification 
  ([type message] (add-notification type message 3000))
  ([type message autohide]
   (async/put! 
     notification-channel 
     {:type type
      :message message
      :visible? true
      :hideable? true
      :adding? true
      :autohide autohide})))


(defn success
  ([message] (success message 3000))
  ([message autohide]
   (add-notification ::success message autohide)))


(defn successf
  [message & args]
  (success (apply goog.string.format message args)))


(defn warning
  ([message] (warning message 3000))
  ([message autohide]
   (add-notification ::warning message autohide)))


(defn warningf
  [message & args]
  (warning (apply goog.string.format message args)))


(defn error
  ([message] (error message 3000))
  ([message autohide]
   (add-notification ::error message autohide)))


(defn errorf
  [message & args]
  (error (apply goog.string.format message args)))


(defmulti render-notification (fn [{:keys [type]}] type))


(defmethod render-notification :default
  [{:keys [type] :as message}]
  (.error js/console "Unknown notifcation renderer for: " type message))


(defn notification-reducer
  [state
   {:keys []
    event-type :type
    :as event}]
  (case event-type
    :toggle/opened? (update state :opened? not)
    :notification/add (update state :notifications conj (:notification event))
    :notification/mark-hide (assoc-in state [:notifications (:idx event) :hidding?] true)
    :notification/hide (->
                         state
                         (assoc-in [:notifications (:idx event) :visible?] false)
                         (update-in [:notifications (:idx event)] dissoc :hidding? :adding?))
    state))


(defn same-notification
  [ap bp]
  (=
    (select-keys ap [:idx :visible? :hidding? :hideable? :adding?])
    (select-keys bp [:idx :visible? :hidding? :hideable? :adding?])))


(defnc Notification
  [{:keys [autohide dispatch idx hideable?] :as props}]
  {:wrap [(memo same-notification)]}
  (let [el (hooks/use-ref nil)
        hide-timeout 300]
    (hooks/use-effect
      :once
      (when (and hideable? (number? autohide) (pos? autohide)) 
        (async/go
          (async/<! (async/timeout (max hide-timeout (- autohide hide-timeout))))
          (dispatch 
            {:type :notification/mark-hide
             :idx idx}))
        (async/go
          (async/<! (async/timeout autohide))
          (dispatch 
            {:type :notification/hide
             :idx idx}))))
    (hooks/use-effect
      [@el]
      (.scrollIntoView @el false))
    (d/div
      {:className "notification-wrapper"
       :key idx
       :ref #(reset! el %)}
      (render-notification props))))


(defnc Store
  [{:keys [frame notifications opened?]
    :or {notifications []
         frame {:bottom  15
                :right 7 
                :position "fixed"
                :width 330
                :max-height 700
                :z-index 1001}
         opened? false}
    :as props}]
  (let [[{:keys [notifications
                 frame
                 opened?]}
         dispatch] 
        (hooks/use-reducer
          notification-reducer 
          {:frame frame
           :notifications notifications
           :opened? opened?})
        ;;
        control-channel (async/chan)
        notifications (if opened? (reverse notifications) notifications)]
    (use-toddler-listener :notification/success (fn [{:keys [message]}] (success message)))
    (use-toddler-listener :notification/error (fn [{:keys [message]}] (error message)))
    (hooks/use-effect
      :once
      ;; Only for development
      ; (add-notification ::warning "Wear protective clothing: Covering your skin with protective clothing such as hats, long sleeves, and pants can help reduce your risk of skin cancer and other sun-related skin damage")
      ; (add-notification ::success "The sun is an essential part of our lives, providing warmth and light that sustain all living things on Earth. Without the sun, there would be no life on our planet.")
      ; (add-notification ::error "prolonged exposure to the sun's ultraviolet (UV) rays can increase the risk of skin cancer, as well as cause sunburn, premature aging, and other skin damage.")
      (async/go-loop
        [[new-notification _] (async/alts! 
                                [notification-channel
                                 control-channel])]
        (if (and
              (some? new-notification)
              (not= ::CLOSE new-notification))
          (do
            (dispatch
              {:type :notification/add
               :notification new-notification})
            (recur (async/alts! 
                     [notification-channel
                      control-channel])))
          (do
            (.log js/console "Closing notifications!")
            :CLOSED)))
      (fn [] (async/put! control-channel ::CLOSE)))
    (<>
      (d/div
        {& props
         :style frame}
        (d/div
          {:class (cond-> ["notifications-wrapper"]
                    (and opened? (not-empty notifications))
                    (conj "opened"))}
          (d/div
            {:class (cond-> ["notifications"]
                      (and opened? (not-empty notifications))
                      (conj "opened"))}
            ;;
            (keep-indexed
              (fn [idx notification]
                (when (or
                        (:visible? notification)
                        opened?)
                  ($ Notification 
                     {:idx idx
                      :key idx
                      :dispatch dispatch
                      :& (cond-> 
                          notification
                          opened? (assoc 
                                    ; :hideable? false
                                    :visible? true))})))
              notifications))))
      (children props))))


(comment
  
  (async/put!
    notification-channel
    {:type :common/warning
     :visible? true
     :hideable? true
     :adding? true
     ; :autohide 2500
     :message "EVO GA"})
  (async/put!
    notification-channel
    {:type :common/success
     :visible? true
     :hideable? true
     :adding? true
     ; :autohide 2500
     :message "EVO GA"})
  (async/put!
    notification-channel
    {:type :common/error
     :visible? true
     :adding? true
     :hideable? true
     :autohide 25000
     :message "EVO GA"}))
