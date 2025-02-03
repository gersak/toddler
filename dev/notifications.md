## NOTIFICATIONS
Notifications are implemented in ```toddler.notifications``` namespace. There are Four
significant constructs.

#### Notification channel
This is channel that expects notifications to arive. You can add notification by using
```clojure
(defn add-notification
  ([type message] (add-notification type message nil))
  ([type message options] (add-notification type message options 3000))
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
     options))))  ;; This notification will autohide after 'autohide' ms
```

#### render
Is multifunction that will receive values from notification channel combined
with additional data that is important to notification *Store*.

Dispatching value for multimethod is value of ```:type``` in
data received from notification-channel.

```clojure
(defmulti render (fn [{:keys [type]}] type))

(defmethod render :default
  [{:keys [type] :as message}]
  (.error js/console "Unknown notifcation renderer for: " type message))
```

#### Notification
Notification is helix component that is responsible for notification lifecycle. Every
notification will go through same lifecycle.
 * **adding** - when notification is added to Notification Store method that is
 responsible for rendering notification will receive :adding? true value in previous code.
 * **visible** - when notification is actually visible. So adding? will be false, and hidding?
 might be false and turn into true after 'autohide' period
 * **hiding** - when autohide has expired, render-notification will receive :hidding? true
 so that hidding animation can be processed

#### Notification Store
Component will create DOM element that will hold notifications received in notification
channel. You can style notification Store, and position it where you like in your user 
interface. 

It doesn't support any class by default  so you can style it the way you like it or
you can use ```toddler.notifications/$default``` style.


Now lets try it out

<div id="notifications-example"></div>


```clojure
(ns toddler.showcase.notifications
  (:require
   [toddler.ui :refer [UI]]
   [toddler.ui.components :as default]
   [helix.core :refer [$ defnc]]
   [helix.hooks :as hooks]
   [toddler.notifications :as notifications]))

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
    ($ ui/row
       {:align :center}
       ($ ui/row
          {:className (css :mt-4 :items-center)}
          ($ ui/text-field
             {:name "MESSAGE"
              :className (css ["& textarea" {:min-height "176px"}])
              :value message
              :on-change set-message!})
          ($ ui/column
             {:className (css :px-4 :pt-5)}
             ($ ui/button {:className "positive" :on-click #(send-message nil)} "Neutral")
             ($ ui/button {:className "positive" :on-click #(send-message :positive)} "Positive")
             ($ ui/button {:className "negative" :on-click #(send-message :negative)} "Negative")
             ($ ui/button {:className "warn" :on-click #(send-message :warn)} "Warning"))))))

(defnc MyApp
  []
  ($ UI
     {:components default/components}
     ($ notifications/Store
        {:class notifications/$default}
        ($ notifications-example))))
```

## CUSTOMIZING NOTIFICATIONS
In previous example we were testing default implementations of notifications
for positive, negative, neutral and warning situations.

What if we wan't to style notifications differently or extend current implementation with some
extra notifications?

As mentioned before with ```render``` multimethod we can extend how new notifications
will be presented. 

In following example we are adding implementation for **:custom/normal**
type of notification. It will show *toddler.fav6.solid/biohazard* icon followed by text message.

Lets go through code
```clojure
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
  ($ ui/row
     {:align :center}
     ($ ui/button
        {:on-click (fn []
                     (notifications/add
                      :custom/normal
                      "Test message for custom notification" nil 5000))}
        "Show")))
```
<div id="custom-notification-example"></div>
