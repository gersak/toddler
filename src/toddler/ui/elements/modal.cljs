(ns toddler.ui.elements.modal
  (:require
   ["react-dom" :as rdom]
   [shadow.css :refer [css]]
   [helix.core
    :refer [$ defnc provider create-context]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [toddler.material.outlined :as outlined]
   [toddler.hooks :as toddler]
   [toddler.ionic :as ionic]
   [toddler.popup :as popup]
   [toddler.layout :as layout]))

(def modal-close-context (create-context))

(def $modal-background
  (css
   :w-full
   :h-full
   {:top "0" :left "0"
    :position "fixed"
    :z-index "1000"
    :backdrop-filter "blur(3px)"
    :background "var(--modal-layer-bg)"}
   ["& .close" :absolute :h-8 :w-8 :m-1
    {:top "0" :right "0"
     :color "var(--modal-close)"}]
   ["&:hover:not(.block) .close"
    {:color "var(--modal-close-hover)"}]))

(defnc modal-background
  [{:keys [class className on-click can-close?] :as props}]
  (let [container-node (hooks/use-context popup/*container*)]
    (rdom/createPortal
     (d/div
      {:key ::background
       :class (cond-> [$modal-background
                       (when-not can-close? "block")]
                (string? class) (conj class)
                (sequential? class) (into class)
                (string? className) (conj className))
       & (dissoc props :class :className :can-close?)}
      (d/div
       ($ outlined/close
          {:className "close"}))
      (c/children props))
     @container-node)))

(def $modal-dialog
  (css
   :flex
   :flex-col
   :rounded-sm
   :pt-10
   :p-8
   :relative
   :border
   :border-normal
   :text-normal
   {:font-size "11px"
    :background "var(--modal-bg)"}
   ["& .title" :text-highlight :text-sm :font-semibold :h-5 :mb-2]
   ["&.success .close" :text-green-900]
   ["&.error .close" :text-red-200]
   ["&.warn .close" :text-red-900]
    ;;
   ["&.success" :text-green-900 :bg-emerald-500]
   ["&.error" :text-red-200 :bg-rose-700]
   ["&.warn" :text-red-900 :bg-amber-400]
   ["& .buttons" :flex :flex-row :justify-end :mt-10 {:gap "0.5rem"}]
   ["& .buttons button"
    :flex :grow :justify-center :h-6
    :button-neutral :uppercase :rounded-sm
    :items-center {:max-width "8.75rem"} :transition-colors]
   ["& .buttons button"
    :border :font-semibold :text-xs
    {:display "flex" :flex "100 0 auto" :width "100px"}]
   ["& .buttons button:hover" :button-neutral-hover]
   ["& .buttons button:hover" :border-normal-hover]
   ["& .buttons button:active" :border-normal-click]
    ; ["&:hover" :border-highlighted]
    ;;
   ["& .buttons button.positive" :button-positive]
   ["& .buttons button.positive:hover" :button-positive-hover]
    ;;
   ["& .buttons button.negative" :button-negative]
   ["& .buttons button.negative:hover" :button-negative-hover]))

(defnc modal-dialog
  [{:keys [class className on-close] :as props}]
  (let [{modal-width :width} (layout/use-container-dimensions)
        width (:width props (min 400 modal-width))
        [can-close? enable-close!] (hooks/use-state true)]
    ($ modal-background
       {:class (css :flex :justify-center)
        :can-close? can-close?
        :onClick (fn []
                   (when (and can-close? (ifn? on-close))
                     (on-close)))}
       (d/div
        {:style {:min-width width}
         :class (cond->
                 [(css
                   :flex
                   :z-20
                   :justify-center
                   :items-center)])
         :onMouseEnter (fn [] (enable-close! false))
         :onMouseLeave (fn [] (enable-close! true))
         & (dissoc props :class)}
        (d/div
         {:class (cond->
                  ["modal-dialog" $modal-dialog]
                   (some? className) (conj className)
                   (string? class) (conj class)
                   (sequential? class) (concat class))}
         (c/children props))))))

(def $strip
  (css
   :flex
   :text-normal
   :bg-normal
   :border
   :border-normal
   {:align-self "flex-end"}))

(defnc modal-strip
  [{:keys [style class max-width on-close]
    :or {max-width 600}
    :as props}]
  (let [{window-width :width
         window-height :height} (toddler/use-window-dimensions)
        {modal-height :height} (layout/use-container-dimensions)
        width (:width props (min max-width window-width))
        _modal (hooks/use-ref nil)
        [can-close? enable-close!] (hooks/use-state true)]
    ($ modal-background
       {:class (css :flex :justify-center)
        :can-close? can-close?
        :onClick (fn []
                   (when (and can-close? (ifn? on-close))
                     (on-close)))}
       (d/div
        {:ref #(reset! _modal %)
         :style (merge
                 style
                 {:width width
                  :height modal-height})
         :onMouseEnter (fn [] (enable-close! false))
         :onMouseLeave (fn [] (enable-close! true))
         :class (cond-> [$strip]
                  (string? class) (conj class)
                  (sequential? class) (concat class))
         & (dissoc props :class :max-width)}
        (provider
         {:context layout/*container-dimensions*
          :value {:width width
                  :height (- window-height 60)}}
         (c/children props))))))

(def $pavement
  (css
   :relative
   :flex
   :grow
   :p-3
   :flex-col
   :box-content
   {:align-self "stretch"}
   ["& .close"
    :text-inactive
    {:width "2em" :height "2em"
     :transition "all .3s ease-in-out"}]
   ["& .close:hover" :text]
   ["& .title" :text-normal :font-bold :flex :items-end {:opacity "0.8"}]))

(defnc modal-pavement
  [{:keys [title on-close max-width]
    :or {max-width 600}
    :as props}]
  (let [{modal-width :width} (toddler/use-window-dimensions)
        width (:width props (min max-width modal-width))]
    ($ modal-strip
       {& props}
       (d/div
        {:class ["modal-pavement" $pavement]}
         ;; Close
        (if title
          (d/div
           {:class [(css
                     :flex
                     :justify-between
                     :pt-4
                        ; :mt-4
                     {:box-sizing "border-box"
                      :flex-grow "1"
                      :max-height "10%"})]}
           (d/div
            {:class "title"}
            (if (string? title)
              (d/pre
               {:style {:max-width (- width 40)}}
               title)
              title))
           (d/div
            {:class [(css
                      :flex
                      :justify-center
                      :items-end)]}
            ($ ionic/close
               {:className "close"
                :onClick (when (fn? on-close) on-close)})))
          (d/div
           {:class [(css
                     :flex
                     :h-7
                     :justify-end
                     {:flex-grow "1"
                      :max-height "10%"})]}
           (d/div
            {:class [(css
                      :flex
                      :justify-center
                      :items-center)]}
            ($ ionic/close
               {:className "close"
                :onClick (when (fn? on-close) on-close)}))))
        ($ layout/Container
           {:className (css :flex :flex-col {:flex-grow "10"})
            :style {:max-height "90%"}}
           (c/children props))))))
