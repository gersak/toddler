(ns toddler.showcase.modal
  (:require
   [shadow.css :refer [css]]
   [toddler.dev :as dev]
   [toddler.layout :as layout]
   [toddler.grid :as grid]
   [toddler.router :as router]
   [toddler.ui :as ui]
   [toddler.ui.components :as components]
   [vura.core :as vura]
   [helix.core :refer [$ defnc <> defhook]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [helix.children :as c]
   [toddler.showcase.content :as content]
   [toddler.hooks :as toddler]
   [toddler.i18n.keyword :refer [add-translations]]))

(add-translations
 (merge
  #:open {:default "Open"
          :hr "Otvori"}
  #:neutral {:hr "Neutralan"
             :default "Neutral"}
  #:positive {:hr "Pozitivan"
              :default "Positive"}
  #:negative {:hr "Negativan"
              :default "Negative"}
  #:warn {:hr "Upozorenje"
          :default "Warning"}
  #:showcase.modal {:default "Modal"
                    :hr "Modalni"}
  #:showcase.modal.background {:default "Click to open modal background"
                               :hr "Pritisni da prikazes modalnu pozadinu"}
  #:showcase.modal.dialog {:default "Click to open dialog"
                           :hr "Pritisni za modalni dijalog"}
  #:showcase.modal.dialog.title {:default "This is title for modal dialog"
                                 :hr "Ovo je naslov za digitalni modalni prozor"}))

(defhook use-close []
  (let [close! (router/use-go-to :toddler.modal)]
    #(close!)))

(defhook use-register [id segment]
  (router/use-component-children
   :toddler.modal
   [{:id id :segment segment}]))

(defnc Background
  []
  (let [show! (router/use-go-to :toddler.modal.background)
        opened? (router/use-rendered? :toddler.modal.background)
        close! (use-close)
        translate (toddler/use-translate)]
    (use-register :toddler.modal.background "background")
    ($ ui/row
       {:label "Background"}
       ($ ui/button
          {:on-click #(show!)}
          (translate :showcase.modal.background))
       (when opened?
         ($ ui/modal-background
            {:on-close close!})))))

(defnc Dialog
  []
  (let [id :toddler.modal.dialog
        show! (router/use-go-to id)
        opened? (router/use-rendered? id)
        close! (use-close)
        translate (toddler/use-translate)
        [context set-context!] (hooks/use-state nil)]
    (use-register :toddler.modal.dialog "dialog")
    ($ ui/row
       {:label "Dialog"}
       ($ ui/button
          {:on-click #(do
                        (set-context! nil)
                        (show!))}
          (translate :neutral))
       ($ ui/button
          {:on-click #(do
                        (set-context! "positive")
                        (show!))
           :class ["positive"]}
          (translate :positive))
       ($ ui/button
          {:on-click #(do
                        (set-context! "negative")
                        (show!))
           :class ["negative"]}
          (translate :button.negative))
       ($ ui/button
          {:on-click #(do
                        (set-context! "warn")
                        (show!))
           :class ["warn"]}
          (translate :warn))
       (when opened?
         ($ ui/modal-dialog
            {:on-close close!
             :width 300
             :style {:max-width 400}
             :className (when context (name context))}
            (d/h3
             {:className "title"}
             (translate :showcase.modal.dialog.title))
            (d/div
             {:class "content"}
             (d/pre
              {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
              (translate :showcase.content.large)))
            (d/div
             {:className "footer"}
             ($ ui/button {:on-click close!} "OK")
             ($ ui/button {:on-click close!} "Cancel")))))))

(defnc Modal
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "30rem"}}
             ($ Background)
             ($ Dialog))))))
