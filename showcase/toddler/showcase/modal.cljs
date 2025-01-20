(ns toddler.showcase.modal
  (:require
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
   :toddler.modal
   [{:id id :segment segment}]))

(defnc Complex
  []
  (use-register :toddler.modal.complex "complex"))

(defnc dialog-example
  [{:keys [opened? context]}]
  (use-register :toddler.modal.background "background")
  (use-register :toddler.modal.dialog "dialog")
  (let [close! (use-close)
        translate (toddler/use-translate)]
    (when opened?
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
          ($ ui/button {:on-click close!} (translate :cancel)))))))

(defnc text-tab
  []
  (let [{:keys [height]} (layout/use-container-dimensions)
        translate (toddler/use-translate)]
    ($ ui/tab
       {:id ::text
        :name "Message"}
       ($ ui/simplebar
          {:style {:height height}
           :shadow true}
          (d/pre
           {:className (css :mt-4 :p-4 :word-break :whitespace-pre-wrap)}
           (translate :showcase.content.large))))))

(defnc complex-dialog-example
  [{:keys [opened? context]}]
  (use-register :toddler.modal.background "background")
  (use-register :toddler.modal.dialog "dialog")
  (let [close! (use-close)
        translate (toddler/use-translate)]
    (when opened?
      ($ ui/modal-dialog
         {:on-close close!
          :width 300
          :className (when context (name context))}
         (d/span
          {:className "title"}
          (translate :showcase.modal.dialog.title))
         ($ layout/Container
            {:class "content"
             :style {:width 400
                     :height 400}}
            ($ ui/tabs
               {:style {:max-height 400}}
               ($ text-tab)
               ($ ui/tab
                  {:id ::form
                   :name "Form"})))
         (d/div
          {:className "footer"}
          ($ ui/button {:on-click close!} (translate :ok))
          ($ ui/button {:on-click close!} (translate :cancel)))))))

(defnc Modal
  {:wrap [(router/wrap-rendered :toddler.modal)]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)
        show-background! (router/use-go-to :toddler.modal.background)
        background-opened? (router/use-rendered? :toddler.modal.background)
        show-dialog! (router/use-go-to :toddler.modal.dialog)
        dialog-opened? (router/use-rendered? :toddler.modal.dialog)
        [context set-context!] (hooks/use-state nil)
        close! (use-close)]
    ($ ui/simplebar
       {:style {:height height
                :width width}
        :shadow true}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "30rem"
                      :min-height 1500}}
             ($ md/watch-url {:url "/doc/en/modal.md"})
             ($ complex-dialog-example
                {:opened? dialog-opened?
                 :context context})
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
                       (translate :warn))))))))))
