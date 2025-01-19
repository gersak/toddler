(ns toddler.showcase.popup
  (:require
   [clojure.core.async :as async]
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.ui :as ui]
   [helix.core :refer [$ defnc <> defhook]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.md.lazy :as md]
   toddler.showcase.content
   [toddler.core :as toddler]
   [toddler.popup :as popup]
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
  (let [close! (router/use-go-to :toddler.popup)]
    #(close!)))

(defhook use-register [id segment]
  (router/use-link
   :toddler.popup
   [{:id id :segment segment}]))

(defnc popup-example
  []
  (let [[opened? set-opened!] (hooks/use-state false)
        [preference set-preference!] (hooks/use-state nil)
        [offset set-offset!] (hooks/use-state 10)]
    (<>
     ;; Row that controls configuration of popup element
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
              {:className (css :my-4)}
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

(defnc Popup
  {:wrap [(router/wrap-rendered :toddler.popup)
          (router/wrap-link
           :toddler.popup
           [{:id ::popup
             :name "Elements"
             :hash "elements"}
            {:id ::modal-dialog
             :name "Modal Dialog"
             :hash "modal-dialog"}
            {:id ::notifications
             :name "Notifications"
             :hash "notifications"}
            {:id ::customizing-notifications
             :name "Customizing Notifications"
             :hash "customizing-notifications"}])]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)
        show-background! (router/use-go-to :toddler.popup.background)
        background-opened? (router/use-rendered? :toddler.popup.background)
        show-dialog! (router/use-go-to :toddler.popup.dialog)
        dialog-opened? (router/use-rendered? :toddler.popup.dialog)
        [context set-context!] (hooks/use-state nil)
        close! (use-close)]
    (use-register :toddler.popup.background "background")
    (use-register :toddler.popup.dialog "dialog")
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "30rem"
                      :min-height 1500}}
             ($ md/watch-url {:url "/doc/en/popup.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "popup-example")}
                ($ popup-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "modal-background-example")}
                ($ ui/row
                   {:align :center}
                   ($ ui/button
                      {:on-click #(show-background!)}
                      (translate :open))
                   (when background-opened?
                     ($ ui/modal-background
                        {:on-close close!}))))
             ;;
             ($ toddler/portal
                {:locator #(.getElementById js/document "modal-dialog-example")}
                (<>
                 ($ ui/row
                    {:position :center}
                    ($ ui/button
                       {:on-click #(do
                                     (set-context! nil)
                                     (show-dialog!))}
                       (translate :neutral))
                    ($ ui/button
                       {:on-click #(do
                                     (set-context! "positive")
                                     (show-dialog!))
                        :class ["positive"]}
                       (translate :positive))
                    ($ ui/button
                       {:on-click #(do
                                     (set-context! "negative")
                                     (show-dialog!))
                        :class ["negative"]}
                       (translate :button.negative))
                    ($ ui/button
                       {:on-click #(do
                                     (set-context! "warn")
                                     (show-dialog!))
                        :class ["warn"]}
                       (translate :warn))
                    (when dialog-opened?
                      ($ ui/modal-dialog
                         {:on-close close!
                          :width 300
                          :className (when context (name context))}
                         (d/span
                          {:className "title"}
                          (translate :showcase.modal.dialog.title))
                         (d/div
                          {:class "content"
                           :style {:max-width 400}}
                          (d/pre
                           {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
                           (translate :showcase.content.large)))
                         (d/div
                          {:className "footer"}
                          ($ ui/button {:on-click close!} (translate :ok))
                          ($ ui/button {:on-click close!} (translate :cancel))))))))
             ($ toddler/portal
                {:locator #(.getElementById js/document "notifications-example")}
                ($ notifications-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "custom-notification-example")}
                ($ custom-notification-example)))))))
