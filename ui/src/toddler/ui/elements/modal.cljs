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
   [toddler.core :as toddler]
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
    :backdrop-filter "var(--modal-layer-bg-filter)"
    :background "var(--modal-layer-bg)"}
   ["&:hover:not(.block)" :cursor-pointer]
   ["& .close" :absolute :h-8 :w-8 :m-1
    {:top "0" :right "0"
     :color "var(--modal-close)"}]
   ["&:hover:not(.block) .close"
    {:color "var(--modal-close-hover)"}]))

(defnc modal-background
  [{:keys [class className can-close? on-close]
    :or {can-close? true
         on-close identity}
    :as props}]
  (let [container-node (hooks/use-context popup/*container*)]
    (when @container-node
      (rdom/createPortal
       (d/div
        {:key ::background
         :on-click (fn [] (when can-close? (on-close)))
         :class (cond-> [$modal-background
                         (when-not can-close? "block")]
                  (string? class) (conj class)
                  (sequential? class) (into class)
                  (string? className) (conj className))
         & (dissoc props :class :className :can-close? :on-click :onClick)}
        (c/children props))
       @container-node))))

(def $modal-dialog
  (css
   :flex
   :flex-col
   :rounded-lg
   :relative
   :border
   :border-normal
   :color
   :text-xs
   {:background "var(--modal-bg)"}
   ["& > .content" :px-8]
   ["& > .title" :px-8 :pt-6 :color+ :text-sm :font-semibold :pb-2]
   ["&.positive" :modal-positive :border-positive]
   ["&.negative" :modal-negative :border-negative]
   ["&.warn" :modal-warn :border-warning]
   ["& > .buttons, & > .footer"
    :pt-4 :px-8 :flex :justify-end :pb-4 {:gap "0.5rem"}]
   ["& > .buttons button, & > .footer button" :mx-0 :my-0]))

(defnc modal-dialog
  [{:keys [class className on-close style] :as props}]
  (let [{modal-width :width} (layout/use-container-dimensions)
        width (:width props (min 400 modal-width))
        [can-close? enable-close!] (hooks/use-state (some? on-close))
        _dialog (hooks/use-ref nil)
        touch? (toddler/use-is-touch?)]
    (hooks/use-effect
      :once
      (when @_dialog
        (if touch?
          true
          (let [rect (.getBoundingClientRect @_dialog)
                {mouse-x :x
                 mouse-y :y} (toddler/get-mouse-position)]
            (when (and
                   (ifn? on-close)
                   (<= mouse-x (.-right rect))
                   (>= mouse-x (.-left rect))
                   (<= mouse-y (.-bottom rect))
                   (>= mouse-y (.-top rect)))
              (enable-close! false))))))
    ($ modal-background
       {:class (css :flex :justify-center)
        :can-close? (or can-close? touch?)
        :on-close on-close}
       (d/div
        {:style {:min-width width}
         :class (cond->
                 [(css
                   :flex
                   :z-20
                   :justify-center
                   :items-center
                   {:animation "fade-in .4s ease-in-out"})])

         & (dissoc props :style :class :className)}
        (d/div
         {:ref #(reset! _dialog %)
          :onMouseEnter (fn [] (when (and (ifn? on-close) (not touch?))
                                 (enable-close! false)))
          :onMouseLeave (fn [] (when (and (ifn? on-close) (not touch?))
                                 (enable-close! true)))
          :style style
          :class (cond->
                  ["modal-dialog" $modal-dialog]
                   (some? className) (conj className)
                   (string? class) (conj class)
                   (sequential? class) (concat class))}
         (when (or can-close? touch?)
           ($ outlined/close
              {:className
               (css :w-6 :h-6
                    :absolute :select-none :pointer-events-none
                    {:color "var(--color-p2)"
                     :top "-24px"
                     :right "-24px"})}))
         (c/children props))))))

(def $strip
  (css
   :flex
   :color
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
         :onMouseEnter (fn [] (when on-close (enable-close! false)))
         :onMouseLeave (fn [] (when on-close (enable-close! true)))
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
    :color-
    {:width "2em" :height "2em"
     :transition "all .3s ease-in-out"}]
   ["& .close:hover" :text]
   ["& .title" :color+ :font-bold :flex :items-end {:opacity "0.8"}]))

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

(def components
  #:modal {:background modal-background
           :strip modal-strip
           :dialog modal-dialog
           ; :avatar-editor modal-avatar-editor
           })
