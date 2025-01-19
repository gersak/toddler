## ELEMENTS
Toddler uses fixed DOM positioning for all popups. It uses react-dom
createPortal to mount popups on ```toddler.popup.Container``` component.

Popup container should be placed as close as posible to your mounted component.
It will create ```<div id="toddler-popups"></div>``` store it in Helix reference
and share it through \***container\*** context.

### POPUP AREA
Component is similar to ```popup/Container```. It will also create *div* element
and render all children inside of this *div* element. It keeps reference to created
*div* DOM element and provides it as **\*area\*** context.

Popup element can then use reference to DOM element to compute where to position on
current screen.

### POPUP ELEMENT
Uses popup area reference to compute where to position. Computing where to position
works in following logic. Popup element expects **preference** property or uses
default **preference** to decide what positions are preferable.

This preference is than used to try and fit popup element on screen and first
position that can be fully visible is valid. ```popup/Element``` doesn't have
width or height by default. This div is adjusted to size of its children, so
whatever you put in, that is responsible for size of popup.

```clojure
(def default-preference
  [#{:bottom :left}
   #{:bottom :right}
   #{:top :left}
   #{:top :right}
   #{:bottom :center}
   #{:top :center}
   #{:left :center}
   #{:right :center}])
```

**offset** prop can be used to move popup element away from ```popup/Area```
DOM element.

By default ```popup/Element``` tracks window resize so that on resize popup
will reposition if necessary.

Popup element will use react *createPortal* function to render popup element
inside of **popup/Container**. So don't forget to create container component
somewhere above in component tree.

#### Example

<div id="popup-example"></div>


```clojure
(defnc popup-example
  []
  (let [[opened? set-opened!] (hooks/use-state false)
        [preference set-preference!] (hooks/use-state nil)
        [offset set-offset!] (hooks/use-state 10)]
    (<>
     ;; Row that controls configuration of popup element
     ;; in example above
     ($ ui/row
        {:className (css {:gap "1em"})}
        ($ ui/dropdown-field
           {:name "Position"
            :value preference
            :on-change set-preference!
            :placeholder "Choose position..."
            :options popup/default-preference})
        (d/div
         {:style {:max-width 100}}
         ($ ui/integer-field
            {:name "Offset"
             :value offset
             :on-change set-offset!})))
     ;; Popup button layout
     ($ ui/row
        {:align :center}
        ($ ui/column
           {:position :center}
           ;; Popup Area defintion
           ($ popup/Area
              ;; That holds one button to toggle
              ;; popup opened/closed
              ($ ui/button
                 {:on-click (fn [] (set-opened! not))}
                 (if opened? "Close" "Open"))
              ;; When it is opened, than show red popup
              (when opened?
                ($ popup/Element
                   {:offset offset
                    :preference (or
                                 (when (some? preference) [preference])
                                 popup/default-preference)}
                   (d/div
                    {:className (css
                                 :w-14 :h-14
                                 :bg-red-600
                                 :border-2
                                 :rounded-lg)})))))))))
```



## MODAL BACKGROUND

As you can see in code bellow, modal-background component will
use react/createPortal to mount modal backround on popup/*container* 
node and pass component children to mounted div (one with :key ::background)

Optional properties for modal-background component include:

 * **can-close?** - controls if on-close can be called
 * **on-close** - when clicked on background will call on-close fn

<div id="modal-background-example"></div>

```clojure
(defnc modal-background
  [{:keys [class className can-close? on-close]
    :or {can-close? true
         on-close identity}
    :as props}]
  (let [container-node (hooks/use-context popup/*container*)]
    (rdom/createPortal
     (d/div
      {:key ::background
       :on-click (fn [] (when can-close? (on-close)))
       :class (cond-> [$modal-background
                       (when-not can-close? " block ")]
                (string? class) (conj class)
                (sequential? class) (into class)
                (string? className) (conj className))
       & (dissoc props :class :className :can-close? :on-click :onClick)}
      (c/children props))
     @container-node)))
```

## MODAL DIALOG

Dialog component will create dialog window and adjust its 
size to inside content. Same as modal-background it supports 
on-close handler, that will be called when user clicks 
outside of dialog window. If omitted you will have to 
handle modal closing by some other means. Like button or something similar.

There are some styled features that come with dialog 
component out of the box. This features are activated 
by adding class to content that is added to ui/modal-dialog component


 * **title** - Stating the purpose of modal-dialog. To emphisize current state
 * **content** - Adding content class will add padding to match footer and title
 * **footer** - Common place to add buttons, so this 
 class will add styles for displaying buttons in common fashion


<div id="modal-dialog-example"></div>


```clojure
($ ui/modal-dialog
 {:on-close close!
  :width 300
  :style {:max-width 400}
  :className (when context (name context))}
 (d/span
  {:className "title"}
  (translate :showcase.modal.dialog.title))
 (d/div
  {:class "content"}
  (d/pre
   {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
   (translate :showcase.content.large)))
 (d/div
  {:className "footer"}
  ($ ui/button {:on-click close!} (translate :ok))
  ($ ui/button {:on-click close!} (translate :cancel))))
```


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
with additional data that is important to notification **Store**.

Dispatching function for multimethod is value of ```:type``` in
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
   [toddler.provider :refer [UI]]
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
