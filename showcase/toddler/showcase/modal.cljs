(ns toddler.showcase.modal
  (:require
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.ui :as ui :refer [!]]
   [helix.core :refer [$ defnc <> defhook provider]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.md.lazy :as md]
   toddler.showcase.content
   [toddler.core :as toddler]
   [toddler.showcase.table :as showcase.table]
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

(defnc dialog-example
  [{:keys [opened? context]}]
  (let [close! (use-close)
        translate (toddler/use-translate)]
    (when opened?
      ($ ui/modal-dialog
         {:on-close close!
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
    (! :tab {:id ::text
             :name "Message"}
       (! :simplebar {:style {:height height}
                      :shadow true}
          (d/pre
           {:className (css :mt-4 :p-4 :word-break :whitespace-pre-wrap)}
           (translate :showcase.content.large))))))

(defnc form-tab
  []
  (let [[{:keys [first-name last-name address
                 city country date-of-birth]} set-state!] (hooks/use-state nil)
        change-field (hooks/use-callback
                       :once
                       (fn [k v]
                         (set-state! assoc k v)))]
    (! :tab {:id ::form
             :name "Form"}
       (d/div
        {:className (css
                     ["& .toddler-row" :my-2 {:gap "0.75em"}])}
        (! :row
           (! :field/input {:name "First Name"
                            :value first-name
                            :on-change #(change-field :first-name %)})
           (! :field/input {:name "Last Name"
                            :value last-name
                            :on-change #(change-field :last-name %)}))
        (! :row
           (! :field/input {:name "Address"
                            :value address
                            :on-change #(change-field :address %)}))
        (! :row
           (! :field/input {:name "City"
                            :value city
                            :on-change #(change-field :city %)}))
        (! :row
           (! :field/input {:name "Country"
                            :value country
                            :on-change #(change-field :country %)}))
        (! :row
           (! :field/date {:name "Date of birth"
                           :value date-of-birth
                           :on-change #(change-field :date-of-birth %)}))))))

(defnc table-tab
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    (! :tab {:id ::table
             :name "Table"}
       (provider
        {:context layout/*container-dimensions*
         :value {:height height
                 :width (- width 64)}}
        (! :table {:rows showcase.table/data
                   :columns showcase.table/columns})))))

(defnc complex-dialog-example
  [{:keys [opened?]}]
  (let [close! (use-close)
        translate (toddler/use-translate)]
    (when opened?
      ($ ui/modal-dialog
         {:on-close close!
          :width 300}
         (d/span
          {:className "title"}
          "Complex Dialog")
         ($ layout/Container
            {:class ["content" (css :mt-4)]
             :style {:width 400
                     :height 400}}
            ($ ui/tabs
               {:style {:max-height 400}}
               ($ text-tab)
               ($ form-tab)
               ($ table-tab)))
         (d/div
          {:className "footer"}
          ($ ui/button {:on-click close!} (translate :ok))
          ($ ui/button {:on-click close!} (translate :cancel)))))))

(defnc Modal
  {:wrap [(router/wrap-rendered :toddler.modal)
          (router/wrap-link
           :toddler.modal
           [{:id ::intro
             :name "Intro"
             :hash "intro"}
            {:id ::modal-dialog
             :name "Dialog"
             :hash "modal-dialog"}
            {:id ::complex-dialog
             :name "Complex"
             :hash "complex-dialog"}
            {:id :toddler.modal.background
             :segment "background"}
            {:id :toddler.modal.dialog
             :segment "dialog"}
            {:id :toddler.modal.complex-dialog
             :segment "complex-dialog"}])]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)
        show-background! (router/use-go-to :toddler.modal.background)
        background-opened? (router/use-rendered? :toddler.modal.background)
        show-dialog! (router/use-go-to :toddler.modal.dialog)
        show-complex! (router/use-go-to :toddler.modal.complex-dialog)
        dialog-opened? (router/use-rendered? :toddler.modal.dialog)
        complex-dialog-opened? (router/use-rendered? :toddler.modal.complex-dialog)
        [context set-context!] (hooks/use-state nil)
        close! (use-close)]
    (! :simplebar {:style {:height height
                           :width width}
                   :shadow true}
       (! :row {:align :center}
          (! :column {:align :center
                      :style {:max-width "30rem"
                              :min-height 1500}}
             ($ md/watch-url {:url "/modal.md"})
             ($ dialog-example
                {:opened? dialog-opened?
                 :context context})
             ($ complex-dialog-example
                {:opened? complex-dialog-opened?})
             ($ toddler/portal
                {:locator #(.getElementById js/document "modal-background-example")}
                (! :row {:align :center}
                   (! :button {:on-click #(show-background!)} (translate :open))
                   (when background-opened?
                     (! :modal/background {:on-close close!}))))
             ;;
             ($ toddler/portal
                {:locator #(.getElementById js/document "modal-dialog-example")}
                (<>
                 (! :row {:position :center}
                    (! :button {:on-click #(do
                                             (set-context! nil)
                                             (show-dialog!))}
                       (translate :neutral))
                    (! :button {:on-click #(do
                                             (set-context! "positive")
                                             (show-dialog!))
                                :class ["positive"]}
                       (translate :positive))
                    (! :button {:on-click #(do
                                             (set-context! "negative")
                                             (show-dialog!))
                                :class ["negative"]}
                       (translate :negative))
                    (! :button {:on-click #(do
                                             (set-context! "warn")
                                             (show-dialog!))
                                :class ["warn"]}
                       (translate :warn)))))
             ($ toddler/portal
                {:locator #(.getElementById js/document "complex-modal-dialog-example")}
                (! :row {:align :center}
                   (! :button
                      {:on-click #(show-complex!)}
                      (translate :open)))))))))
