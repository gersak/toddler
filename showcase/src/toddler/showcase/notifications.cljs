(ns toddler.showcase.notifications
  (:require
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.ui :as ui :refer [!]]
   [helix.core :refer [$ defnc defhook]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.md.lazy :as md]
   toddler.showcase.content
   [toddler.core :as toddler]
   [toddler.notifications :as notifications]
   [toddler.fav6.solid :as solid]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 (merge
  #:showcase.modal {:default "Modal"
                    :hr "Modalni"}
  #:showcase.modal.background {:default "modal background"
                               :hr "modalna pozadina"}
  #:showcase.modal.dialog {:default "modal dialog"
                           :hr "Pritisni za modalni dijalog"}
  #:showcase.modal.dialog.title {:default "This is title for modal dialog"
                                 :hr "Ovo je naslov za digitalni modalni prozor"}))

(defhook use-close []
  (let [close! (router/use-go-to :toddler.modal)]
    #(close!)))

(defhook use-register [id segment]
  (router/use-link
   :toddler.popup
   [{:id id :segment segment}]))

(defnc notifications-example
  []
  (let [[message set-message!] (hooks/use-state "")
        send-message (hooks/use-callback
                       [message]
                       (fn [context]
                         ((case context
                            :positive notifications/positive
                            :negative notifications/negative
                            :warn notifications/warning
                            notifications/neutral)
                          (or (not-empty message) "You should type something in :)")
                          3000)
                         (set-message! "")))]
    #_(hooks/use-effect
        :once
        (notifications/positive "Message" 0)
        (notifications/negative "Message" 0)
        (notifications/warning "Message" 0)
        (notifications/neutral "Message" 0))
    (! :row {:align :center}
       (! :row {:className (css :mt-4 :items-center)}
          (! :field/text {:name "MESSAGE"
                          :className (css ["& textarea" {:min-height "176px"}])
                          :value message
                          :on-change set-message!})
          (! :column
             {:className (css :px-4 :pt-5)}
             (! :button {:on-click #(send-message nil)} "Neutral")
             (! :button {:className "positive" :on-click #(send-message :positive)} "Positive")
             (! :button {:className "negative" :on-click #(send-message :negative)} "Negative")
             (! :button {:className "warn" :on-click #(send-message :warn)} "Warning"))))))

(defmethod notifications/render :custom/normal
  [{:keys [message]}]
  (d/div
   {:className (css
                :bg-yellow-400
                :text-black
                :border-black
                :border-2
                :px-3
                :py-3
                :rounded-lg
                :flex
                :items-center
                ["& .icon" :mr-2 {:font-size "24px"}]
                ["& .message" :font-semibold :text-sm])}
   (d/div
    {:className "icon"}
    ($ solid/biohazard))
   (d/pre
    {:className "message"}
    message)))

(defnc custom-notification-example
  []
  (! :row {:align :center}
     (! :button
        {:on-click (fn []
                     (notifications/add
                      :custom/normal
                      "Test message for custom notification" nil 5000))}
        "Show")))

(defnc Notifications
  {:wrap [(router/wrap-rendered :toddler.notifications)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    (! :simplebar
       {:style {:height height
                :width width}}
       (! :row {:align :center}
          (! :column
             {:align :center
              :style {:max-width "30rem"
                      :min-height 1500}}
             ($ md/watch-url {:url "/notifications.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "notifications-example")}
                ($ notifications-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "custom-notification-example")}
                ($ custom-notification-example)))))))
