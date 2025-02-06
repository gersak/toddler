(ns toddler.showcase.popup
  (:require
   [clojure.core.async :as async]
   [shadow.css :refer [css]]
   [toddler.layout :as layout]
   [toddler.router :as router]
   [toddler.ui :as ui :refer [!]]
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
     (! :row
        {:className (css {:gap "1em"})}
        (! :field/dropdown
           {:name "Position"
            :value preference
            :on-change set-preference!
            :placeholder "Choose position..."
            :options popup/default-preference})
        (d/div
         {:style {:max-width 100}}
         (! :field/integer
            {:name "Offset"
             :value offset
             :on-change set-offset!})))
     ;; Popup button layout
     (! :row {:align :center}
        (! :column {:position :center
                    :style {:align-items "center"}}
           ;; Popup Area defintion
           ($ popup/Area
              {:className (css :my-4)}
              ;; That holds one button to toggle
              ;; popup opened/closed
              (! :button {:on-click (fn [] (set-opened! not))}
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

(defn tooltip-example
  []
  (let [[state set-state!] (hooks/use-state "neutral")]
    (<>
     (! :row
        (! :field/dropdown
           {:value state
            :on-change set-state!
            :placeholder "Choose context..."
            :options ["neutral"
                      "positive"
                      "negative"
                      "warning"]}))
     (! :row {:align :center}
        (! :column {:align :center}
           (! :tooltip
              {:message (case state
                          "positive" "I'm happy"
                          "negative" "I don't feel so good"
                          "warning"  (d/pre "I'm affraid that something\nbad might happen")
                          "Just business as usual")
               :className state}
              (d/div
               {:className (css
                            :font-bold
                            :text-base
                            :text-center
                            :cursor-default
                            :my-5)}
               "Hiii if you hover over me... Than")))))))

(defnc Popup
  {:wrap [(router/wrap-rendered :toddler.popup)
          (router/wrap-link
           :toddler.popup
           [{:id ::general
             :name "In general"
             :hash "intro"}
            {:id ::tooltip
             :name "Tooltip"
             :hash "tooltip"}])]}
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)]
    (! :simplebar
       {:style {:height height
                :width width}
        :shadow true}
       (! :row {:align :center}
          (! :column
             {:align :center
              :style {:max-width "30rem"
                      :min-height 1500}}
             ($ md/watch-url {:url "/popup.md"})
             ($ toddler/portal
                {:locator #(.getElementById js/document "popup-example")}
                ($ popup-example))
             ($ toddler/portal
                {:locator #(.getElementById js/document "tooltip-example")}
                ($ tooltip-example)))))))
