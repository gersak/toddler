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
   [toddler.notifications :as notifications]
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

(defnc Complex
  []
  (use-register :toddler.modal.complex "complex"))

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
                {:locator #(.getElementById js/document "modal-background-example")}
                ($ ui/row
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
                ($ notifications-example)))))))
