(ns toddler.notifications
  (:require
   clojure.string
   goog.string.format
   [shadow.css :refer [css]]
   [clojure.core.async :as async]
   [helix.core
    :refer [defnc $ <>
            memo create-context
            provider]]
   [helix.children :refer [children]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [toddler.material.outlined :as outlined]))

(defonce notification-channel (async/chan 1000))

(defn add
  ([type message] (add type message nil))
  ([type message options] (add type message options 3000))
  ([type message options autohide]
   (async/put!
    notification-channel
    (merge
     {:type type
      :message message
      :visible? true
      :hideable? true
      :adding? true
      :autohide autohide}
     options))))

(defn neutral
  ([message] (neutral message 3000))
  ([message autohide]
   (add ::neutral message nil autohide)))

(defn positive
  ([message] (positive message 3000))
  ([message autohide]
   (add ::positive message {:class "positive"} autohide)))

(defn warning
  ([message] (warning message 3000))
  ([message autohide]
   (add ::warning message {:class "warning"} autohide)))

(defn negative
  ([message] (negative message 3000))
  ([message autohide]
   (add ::negative message {:class "negative"} autohide)))

(defmulti render (fn [{:keys [type]}] type))

(defmethod render :default
  [{:keys [type] :as message}]
  (.error js/console "Unknown notifcation renderer for: " type message))

(defn render-default
  [{:keys [id visible? hideable? message dispatch hidding? adding? class className]
    :as notification}]
  (d/div
   {:key id
    :class (cond-> ["notification"]
             (and visible? adding? (not hidding?))
             (conj "new")
                         ;;
             (and visible? (not adding?) (not hidding?))
             (conj "show")
                         ;;
             hidding? (conj "hide")
             (string? class) (conj class)
             (string? className) (conj className)
             (sequential? class) (into class))}
   (d/div
    {:class "close"
     :style {:visibility
             (if (or
                  (not hideable?)
                  (not visible?))
               "hidden"
               "visible")}}
    ($ outlined/close
       {:onClick #(dispatch
                   {:type :notification/hide
                    :notification notification})}))
   (d/div
    {:class "content"}
    (d/div
     {:class "message"}
     (d/pre message)))))

(defmethod render ::neutral [data] (render-default data))
(defmethod render ::positive [data] (render-default (assoc data :class "positive")))
(defmethod render ::negative [data] (render-default (assoc data :class "negative")))
(defmethod render ::warning [data] (render-default (assoc data :class "warning")))

(defn notification-reducer
  [{:keys [notifications] :as state}
   {event-type   :type
    {:keys [id]} :notification
    :as event}]
  (let [idx (.indexOf (map :id notifications) id)]
    (case event-type
      :toggle/opened? (update state :opened? not)
      :notification/add (update state :notifications conj (assoc (:notification event) :id (random-uuid)))
      :notification/mark-hide (assoc-in state [:notifications idx :hidding?] true)
      :notification/mark-visible (assoc-in state [:notifications idx :adding?] false)
      :notification/hide (->
                          state
                          (assoc-in [:notifications idx :visible?] false)
                          (update-in [:notifications idx] dissoc :hidding? :adding?))
      state)))

(defn same-notification
  [ap bp]
  (=
   (select-keys ap [:idx :visible? :hidding? :hideable? :adding?])
   (select-keys bp [:idx :visible? :hidding? :hideable? :adding?])))

(def -hide-timeout- (create-context))
(def -new-timeout- (create-context))

(defnc Notification
  {:wrap [(memo same-notification)]}
  [{:keys [adding? autohide dispatch id hideable?] :as props}]
  (let [el (hooks/use-ref nil)
        hide-timeout (hooks/use-context -hide-timeout-)
        new-timeout (hooks/use-context -new-timeout-)]
    (hooks/use-effect
      :once
      (when adding?
        (async/go
          (async/<! (async/timeout (max new-timeout (- autohide new-timeout))))
          (dispatch
           {:type :notification/mark-visible
            :notification props})))
      (when (and hideable? (number? autohide) (pos? autohide))
        (async/go
          (async/<! (async/timeout (max hide-timeout (- autohide hide-timeout))))
          (dispatch
           {:type :notification/mark-hide
            :notification props}))
        (async/go
          (async/<! (async/timeout autohide))
          (dispatch
           {:type :notification/hide
            :notification props}))))
    (hooks/use-effect
      [@el]
      (.scrollIntoView @el false))
    (d/div
     {:className "notification-wrapper"
      :key id
      :ref #(reset! el %)}
     (render props))))

(defnc Store
  "Returns component that will render notifications that are
  sent to notification-channel.

   :frame - used to position notification store and style it
   :notifications - if some init notifications are available
   :opened? - parameter that you can use to open notification
              and view notification history

   :new-timeout  - how long will notification receive :adding? true
   :hide-timeout - how long will notification receive :hidding? true
  
  IMPORTANT: Put Store as close as posible to your mounted component.
  Preferable just after you have initialized UI component."
  [{:keys [frame notifications opened? hide-timeout new-timeout]
    :or {notifications []
         frame {:bottom  15
                :right 7
                :position "fixed"
                :width 330
                :max-height 700
                :z-index 1001}
         hide-timeout 100
         new-timeout 100
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
    (hooks/use-effect
      :once
      ;; Only for development
      ; (add ::warning "Wear protective clothing: Covering your skin with protective clothing such as hats, long sleeves, and pants can help reduce your risk of skin cancer and other sun-related skin damage")
      ; (add ::success "The sun is an essential part of our lives, providing warmth and light that sustain all living things on Earth. Without the sun, there would be no life on our planet.")
      ; (add ::error "prolonged exposure to the sun's ultraviolet (UV) rays can increase the risk of skin cancer, as well as cause sunburn, premature aging, and other skin damage.")
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
    (provider
     {:context -hide-timeout-
      :value hide-timeout}
     (provider
      {:context -new-timeout-
       :value new-timeout}
      (<>
       (d/div
        {& (dissoc props :frame :notifications :opened?)
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
                   :& (cond-> notification
                        opened? (assoc
                                         ; :hideable? false
                                 :visible? true))})))
           notifications))))
       (children props))))))

(def $default
  (css
   {:color "var(--notification-text)"}
   ["& .notification-wrapper" :py-1]
   ["& .notification"
    :flex
    :rounded-md
    :grow
    :py-3
    :px-4
    :relative
    :border
    :border-normal
    :bg-normal+
    :shadow-lg
    :items-center
    {:z-index "200"
     :transition "color .3s ease-in-out"}]
    ;;
   ["& .notification .icon"
    :flex
    :items-center
    {:order 2}]
   ["& .notification pre" :word-break :whitespace-pre-wrap]
    ;;
   ["& .notification .content"
    :flex :items-center
    :mr-2
    :grow
    {:order 2}
    :text-xxs
    :font-medium]
    ;;
   ["& .notification .close"
    :absolute
    :top-3
    :right-3
    :flex
    :items-center
    :ml-3 :pl-3
    {:order 3
     :opacity "0.5"
     :justify-self "flex-end"
     :transition "color .3s ease-in-out"}
    :text-sm]
   ["& .notification" {:background-color "var(--notification-neutral)"}]

   ["& .notification .close svg" :w-4 :h-4]
    ;;
   ["& .notification .close:hover" :cursor-pointer {:opacity "1"}]
    ;;
   ["& .notification.negative" {:background-color "var(--notification-negative)"
                                :border-color  "var(--border-negative)"}]
   ["& .notification.positive" {:background-color "var(--notification-positive)"
                                :border-color "var(--border-positive)"}]
   ["& .notification.warning" {:background-color "var(--notification-warn)"
                               :border-color "var(--border-warning)"}]))

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
