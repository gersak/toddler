;; Copyright (C) Neyho, Inc - All Rights Reserved
;; n-Unauthorized copying of this file, via any medium is strictly prohibited
;; Proprietary and confidential
;; Writtenby Robert Gersak <robi@neyho.com>, June 2019

(ns toddler.elements.notifications
  (:require-macros 
    [cljs.core.async.macros :refer [go] :as async])
  (:require
    clojure.string
    goog.string.format
    ["@fortawesome/free-solid-svg-icons" 
     :refer [faExclamationTriangle
             faExclamation
             faCheck
             faQuestion
             faTimes
             faLightbulb
             faChevronUp
             faChevronDown]]
    [toddler.hooks :refer [use-toddler-listener]]
    [toddler.interactions :as interactions]
    [helix.styled-components :refer [defstyled --themed]]
    [helix.children :as c]
    [helix.core :refer [defnc $ memo]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [clojure.core.async :as async]))


(defonce state
  (atom
    {:notifications []
     :frame {:bottom -3 
             :right 7 
             :width 330
             :max-height 700}
     :opened? false}))

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


(defn notify-success
  ([message] (notify-success message 3000))
  ([message autohide]
   (add-notification :common/success message autohide)))


(defn notify-successf
  [message & args]
  (notify-success (apply goog.string.format message args)))

(defn notify-warning
  ([message] (notify-warning message 3000))
  ([message autohide]
   (add-notification :common/warning message autohide)))

(defn notify-warningf
  [message & args]
  (notify-warning (apply goog.string.format message args)))


(defn notify-error
  ([message] (notify-error message 3000))
  ([message autohide]
   (add-notification :common/error message autohide)))

(defn notify-errorf
  [message & args]
  (notify-error (apply goog.string.format message args)))



(defn notification-icon [t]
  (let [t' ((comp clojure.string/lower-case (fnil name "")) t)]
    ($ interactions/fa
       {:icon (case t' 
                 "warning" faExclamationTriangle
                 "error" faExclamation 
                 "success" faCheck
                 faQuestion)})))

(defn notification-message [m]
  (d/div 
    {:class "notification-message"}
    (d/pre m)))

(defnc NotificationModal
  [{:keys [message
           className]
    message-type :type
    :as props}]
  (d/div 
    {:class className}
    (d/div 
      {:class "notification-content"}
      (notification-icon message-type)
      (notification-message message))
    (c/children props)))


; (defn --themed-notification
;   [{:keys [theme]
;     message-type :type}]
;   (case (:name theme)
;     {:background-color "white"
;      :border "10px solid rgba(255, 255, 255, 0.7)"
;      :box-shadow "0px 0px 9px -1px #353535"
;      (str ".notification-content " interactions/fa)
;      {:font-size "40"
;       :cursor "default"
;       :padding-right 25
;       :color (case message-type
;                "warning" default/dark-yellow
;                "error" default/red
;                "success" default/green
;                default/blue)}}))

(defstyled notification-modal NotificationModal
  {:display "flex"
   :flex-direction "column"
   :justify-content "space-between"
   :min-width 300
   
   :border-radius 2
   
   :background-clip "padding-box"
   :max-height "calc(100% - 100px)"
   :position "fixed"
   :top "50%"
   :left "50%"
   :right "auto"
   :bottom "auto"
   :transform "translate(-50%, -50%)"
   :z-index "990"
   :padding "20px 30px 30px"
   
   :transition "all 0.5s ease-in-out"
   ;;
   ".notification-content" 
   {:display "flex"
    :flex-direction "row"
    :align-items "center" 
    :justify-content "center"
    :margin "0 0 15px"
    :font-size 16
    :position "relative"
    :min-height 50
    :p {:border-left "1px solid"
        :padding-left 10}

    :span {:font-weight "600"}}
   ;;
   ".notification-children" 
   {:font-size 14
    :display "flex"
    :flex-grow 2
    :flex-direction "column"
    :justify-content "flex-start"
    :overflow-y "auto"
    :border-radius 3}
   ".notification-actions" 
   {:display "flex"
    :justify-content "flex-end"}}
  ;;
  --themed)

(defmulti render-notification (fn [{:keys [type]}] type))

(def in-effect "zoomIn")
(def out-effect "fadeOut")

(defmethod render-notification :common/error
  [{:keys [visible? hideable? message idx dispatch hidding? adding?]}]
  (d/div 
    {:key idx
     :class (clojure.string/join
              " "
              (cond-> ["notification" "error" "animated" "faster"]
                (and visible? adding? (not hidding?)) 
                (conj in-effect)
                ;;
                (and visible? (not adding?) (not hidding?)) 
                (conj "fadeIn") 
                ;;
                hidding? (conj out-effect)))}
    (d/div 
      {:class "close"
       :style {:visibility 
               (if (or
                     (not hideable?)
                     (not visible?))
                 "hidden"
                 "visible")}}
      ($ interactions/fa 
         {:icon faTimes
          :onClick #(dispatch
                      {:type :notification/hide
                       :idx idx})}))
    (d/div 
      {:class "type"}
      ($ interactions/fa
         {:icon faExclamation}))
    (d/div 
      {:class "content"} 
      (d/div {:class "message"} (d/span message)))))

(defmethod render-notification :common/warning
  [{:keys [visible? hidding? hideable? message idx dispatch adding?]}]
  (d/div 
    {:key idx
     :class (clojure.string/join
              " " 
              (cond-> ["notification" "warning" "animated" "faster"]
                (and visible? adding? (not hidding?)) 
                (conj in-effect)
                ;;
                (and visible? (not adding?) (not hidding?)) 
                (conj "fadeIn") 
                ;;
                hidding? (conj out-effect)))}
    (d/div 
      {:class "close"
       :style {:visibility 
               (if (or
                     (not hideable?)
                     (not visible?))
                 "hidden"
                 "visible")}}
      ($ interactions/fa
         {:icon faTimes
          :onClick #(dispatch
                      {:type :notification/hide
                       :idx idx})}))
    (d/div 
      {:class "type"}
      ($ interactions/fa {:icon faExclamationTriangle}))
    (d/div 
      {:class "content"} 
      (d/div {:class "message"} (d/span message)))))


(defmethod render-notification :common/success
  [{:keys [visible? hideable? hidding? message idx dispatch adding?]}]
  (d/div 
    {:key idx
     :class (clojure.string/join 
              " " 
              (cond-> ["notification" "success" "faster" "animated"]
                (and visible? adding? (not hidding?)) 
                (conj in-effect)
                ;;
                (and visible? (not adding?) (not hidding?)) 
                (conj "fadeIn") 
                ;;
                hidding? (conj out-effect)))}
    (d/div 
      {:class "close"
       :style {:visibility 
               (if (or
                     (not hideable?)
                     (not visible?))
                 "hidden"
                 "visible")}}
      ($ interactions/fa
        {:icon faTimes
         :onClick #(dispatch
                     {:type :notification/hide
                      :idx idx})}))
    (d/div 
      {:class "type"}
      ($ interactions/fa {:icon faCheck}))
    (d/div 
      {:class "content"} 
      (d/div 
        {:class "message"}
        (d/span message)))))


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
  (let [el (hooks/use-ref nil)] 
    (hooks/use-effect
      :once
      (when (and hideable? (number? autohide) (pos? autohide)) 
        (go
          (async/<! (async/timeout (max 200 (- autohide 200))))
          (dispatch 
            {:type :notification/mark-hide
             :idx idx}))
        (go
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


(defnc NotificationStore
  [{:keys [className]}]
  (let [[{:keys [notifications
                 frame
                 opened?]
          :as current-state}
         dispatch] 
        (hooks/use-reducer
          notification-reducer 
          @state)
        ;;
        control-channel (async/chan)
        notifications (if opened? (reverse notifications) notifications)]
    (use-toddler-listener :notification/success (fn [{:keys [message]}] (notify-success message)))
    (use-toddler-listener :notification/error (fn [{:keys [message]}] (notify-error message)))
    (hooks/use-effect
      :always
      (reset! state current-state))
    (hooks/use-effect
      :once
      (async/go-loop [[new-notification _] (async/alts! 
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
    (d/div
      {:class (cond-> [className]
                (and opened? (not-empty notifications))
                (conj "opened"))
       :style frame}
      (d/div
        {:class (cond-> ["notifications-wrapper"]
                  (and opened? (not-empty notifications))
                  (conj "animated" "fadeIn"))}
        (d/div
          {:class (cond-> ["notifications"]
                    (and opened? (not-empty notifications))
                    (conj "animated" "fadeIn"))}
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
                    & (cond-> 
                        notification
                        opened? (assoc 
                                  :hideable? false
                                  :visible? true))})))
            notifications)))
      (when (not-empty notifications) 
        (d/div
          {:class "notifications-toggle"}
          (d/button
            {:onClick #(dispatch {:type :toggle/opened?})}
            ($ interactions/fa
               {:icon (if opened?
                        faChevronUp
                        faChevronDown)})
            "Notifications"))))))


(defstyled notification-store NotificationStore
  {:position "fixed"
   ; :bottom 0
   ; :right 20
   :display "flex"
   :flex-direction "column"
   :justify-content "flex-end"
   "&.opened"
   {".notifications-wrapper" 
    {:visibility "visible"
     :background-color "#e1f9ff"
     :box-shadow "0px 3px 7px 3px #00000055"
     ".notifications" 
     {:padding "0px"
      :overflow-y "auto"}}
    ".notifications-toggle" 
    {:opacity "1"}}
   ".notifications-wrapper" 
   {:display "flex"
    :flex-direction "column"
    :justify-content "flex-end"
    :margin-bottom 10
    :border-radius 10 
    :padding "10px 2px 4px 2px"
    :background-color "transparent"
    ".notifications" 
    {:padding-left 18
     :overflow "hidden"}}
   ".notifications" 
   {:max-height 700
    :position "relative"
    :overflow-x "hidden"
    :flex-direction "column"
    :justify-content "flex-end"}
   ".notifications-toggle" 
   {:display "flex"
    :opacity ".3"
    :flex-direction "row"
    :justify-content "flex-end"
    :font-size 10
    :pointer-events "none"
    "&:hover"
    {:opacity 1}
    :button 
    {:pointer-events "all"
     :i {:margin-right 5}}}
   ".notification-wrapper" {:padding 5}
   "&:not(.opened) .notification" {:box-shadow "0px 5px 4px -3px black"}
   ".notification" 
   {:z-index "100"
    :display "flex"
    :align-items "center"
    :background "white"
    :padding 8
    :border-radius 5
    ".type"
    {:order 1
     :display "flex"
     :justify-content "center"
     :align-items "center"
     :margin-right 6
     :width 20}
    ".close" 
    {:justify-self "flex-end"
     :order 3
     :i 
     {:margin "0px 2px"
      :font-size 10
      "&:hover" 
      {:cursor "pointer"
       :color "black"}}}
    ".content" 
    {:order 1
     :flex-grow "1"
     :width 200}
    ".message" 
    {:word-wrap "break-word"
     :font-size 10
     :p {:margin "3px 0"}}
    "&.error"
    {:background-color "#fe9eb4"
     :color "#c4022f"}
    "&.success"
    {:background-color "#90edc8"
     :color "#2b7859"}
    "&.warning"
    {:background-color "#fff5b8"
     :color "#636154"}}})


(defnc Tip
  [{:keys [title message children style className]}]
  (let [[hidden? hide] (hooks/use-state true)
        visible? (not hidden?)
        [hovering set-hover] (hooks/use-state false)] 
    (d/div
      {:style style
       :class className}
      ($ interactions/fa
         {:icon faLightbulb
          :style #js {:visibility (if (not (or visible? hovering))
                                    "hidden"
                                    "visible")}
          :pull "left"})
     (d/div 
      {:class "eywa-tip-content"}
      (when title 
        (d/div 
          {:class "eywa-tip-title"
           :onClick #(hide not)
           :on-mouse-enter #(set-hover true)
           :on-mouse-leave #(set-hover false)} 
          title))
      (d/pre 
        {:class (clojure.string/join 
                  (cond-> ["eywa-tip-message"]
                    visible? (conj "visible")))}
       message)
      (when visible? children)))))

(defstyled tip Tip
  {:display "flex"
   :flex-direction "row"
   :margin "10px 0 5px 0"
   (str interactions/fa) {:height 20}
   ".eywa-tip-content" 
   {:display "flex"
    :flex-direction "column"
    :justify-content "flex-start"
    ".eywa-tip-title" 
    {:font-size 14
     :font-weight "600"
     ":hover" {:cursor "pointer"}}
    ".eywa-tip-message" 
    {:margin-top 2
     :margin-bottom 2
     :font-size 10
     :max-height 0
     :overflow "hidden"
     :transition "max-height 0.25s ease-out"
     ".visible" 
     {:max-height 500
      :transition "max-height 0.45s ease-in"}}}}
  --themed
  #_(fn [{:keys [theme]}]
    (case (:name theme) 
      {:color default/color
       (str interactions/fa) {:color default/dark-yellow}})))


(comment
  (add-notification :common/warning "Bok Martina! Greta je kakala!")
  (add-notification :common/error "SRANJE!!!")
  (go
    (dotimes [n 5]
      (async/<! (async/timeout 800))
      (async/put!
        notification-channel
        {:type (rand-nth [:common/warning :common/success :common/error])
         :visible? true
         :hideable? true
         :adding? true
         :autohide 4000
         :message (str "Message " n)})))
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
