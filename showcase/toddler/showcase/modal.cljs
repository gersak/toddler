(ns toddler.showcase.modal
  (:require
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.ui :as ui]
   [toddler.util :as util]
   [helix.core :refer [$ defnc <> defhook]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   toddler.showcase.content
   [toddler.hooks :as toddler]
   [toddler.i18n.keyword :refer [add-translations]]))

(def $info
  (css :mt-4 :text-xs
       ["& h4" :uppercase]
       ["& p" :mt-2]
       ["& br" {:height "8px"}]
       ["& ul" :ml-4 {:list-style-type "disc"}]))

(defnc ModalInfo
  []
  (let [locale (toddler/use-current-locale)]
    (case locale
      (d/div
       {:className (css :text-xs {:max-width "400px"}
                        ["& b, & strong" :font-semibold])}
       (d/h4 "Hello,")
       (d/br)
       (d/p
        "This is place to showcase " (d/b "modal")
        " components. There are just few modal building blocks. Modal "
        (d/b "background") ", " (d/b "dialog"), " and " (d/b "strip") ".")
       (d/p
        "Modal dialog and modal strip use modal background component to display"
        " components that you will design and implement.")))))

(defnc BackgroundInfo
  []
  (let [locale (toddler/use-current-locale)
        translate (toddler/use-translate)]
    (case locale
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.background))
       (d/p
        "As you can see in code bellow, modal-background component "
        "will use react/createPortal to mount modal backround "
        "on popup/*container* node and pass component children to "
        "mounted " (d/b "div") " (one with :key ::background)")
       (d/p
        "Optional properties for modal-background component include:")
       (d/ul
        (d/li (d/strong "can-close?") " - controls if on-close can be called")
        (d/li (d/strong "on-close")  " - when clicked on background will call on-close fn"))))))

(defnc DialogInfo
  []
  (let [locale (toddler/use-current-locale)
        translate (toddler/use-translate)]
    (case locale
      (d/div
       {:className $info}
       (d/h4 (translate :showcase.modal.dialog))
       (d/p
        "As you can see in code bellow, modal-background component "
        "will use react/createPortal to mount modal backround "
        "on popup/*container* node and pass component children to "
        "mounted " (d/b "div") " (one with :key ::background)")
       (d/p
        "Optional properties for modal-background component include:")
       (d/ul
        (d/li (d/strong "can-close?") " - controls if on-close can be called")
        (d/li (d/strong "on-close")  " - when clicked on background will call on-close fn"))))))

(add-translations
 (merge
  #:showcase.modal {:default "Modal"
                    :hr "Modalni"}
  #:showcase.modal.info {:default ModalInfo}
  #:showcase.modal.background.info {:default BackgroundInfo}
  #:showcase.modal.dialog.info {:default DialogInfo}
  #:showcase.modal.background {:default "modal background"
                               :hr "Pritisni da prikazes modalnu pozadinu"}
  #:showcase.modal.dialog {:default "modal dialog"
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
    (<>
     ($ (translate :showcase.modal.background.info))
     ($ ui/row
        ($ ui/button
           {:on-click #(show!)}
           (translate :open))
        (when opened?
          ($ ui/modal-background
             {:on-close close!})))
     (d/pre
      (d/code
       {:class ["clojure"]}
       "(defnc modal-background
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
                       (when-not can-close? \" block \")]
                (string? class) (conj class)
                (sequential? class) (into class)
                (string? className) (conj className))
       & (dissoc props :class :className :can-close? :on-click :onClick)}
      (c/children props))
     @container-node)))")))))

(defnc Dialog
  []
  (let [id :toddler.modal.dialog
        show! (router/use-go-to id)
        opened? (router/use-rendered? id)
        close! (use-close)
        translate (toddler/use-translate)
        [context set-context!] (hooks/use-state nil)]
    (use-register :toddler.modal.dialog "dialog")
    (<>
     ($ (translate :showcase.modal.dialog.info))
     ($ ui/row
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
              ($ ui/button {:on-click close!} (translate :cancel))))))
     (d/pre
      (d/code
       "($ ui/modal-dialog
 {:on-close close!
  :width 300
  :style {:max-width 400}
  :className (when context (name context))}
 (d/span
  {:className \"title\"}
  (translate :showcase.modal.dialog.title))
 (d/div
  {:class \"content\"}
  (d/pre
   {:className (css :mt-4 :word-break :whitespace-pre-wrap)}
   (translate :showcase.content.large)))
 (d/div
  {:className \"footer\"}
  ($ ui/button {:on-click close!} \"OK\")
  ($ ui/button {:on-click close!} \"Cancel\")))")))))

(defnc Complex
  []
  (use-register :toddler.modal.complex "complex"))

(defnc Modal
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "30rem"}}
             ($ (translate :showcase.modal.info))
             ($ Background)
             ($ Dialog)
             ($ Complex))))))
