(ns toddler.elements.modal
  (:require
    clojure.string
    ["toddler-icons$default" :as icon]
    ["react" :as react]
    ["react-dom" :as rdom]
    [vura.core :refer [round-number]]
    [helix.styled-components :refer [defstyled --themed]]
    [helix.core :as hx :refer [defnc defhook $ <>]]
    [helix.dom :as d]
    [helix.hooks :as hooks]
    [helix.children :as c]
    [toddler.interactions :as interactions]
    [toddler.hooks :refer [use-dimensions]]))



(defnc ModalBackground
  [{:keys [children
           on-click]}]
  (hooks/use-effect
      :once
      (set! (.. js/document -body -className) "modal_open")
      (fn []
        (set! (.. js/document -body -className) "")))
  (hooks/use-effect
    :once
    (set! (.. js/document -body -className) "modal_open")
    (fn []
      (set! (.. js/document -body -className) "")))
  (d/div
    {:class "modal-dialog"
     :on-click (fn [e] (when (fn? on-click) (on-click e)))}
    children))

(defn calculate-position [{:keys [width height]}]
  (when (and width height)
    (let [[ww wh] [(.-innerWidth js/window) (.-innerHeight js/window)]

          [wcw wch] (map #(/ % 2) [ww wh])
          [top left] [(- wch (/ height 2) ) (- wcw (/ width 2))]]
      {:top top
       :left left})))

(defnc ModalBox
  [{:keys [class] :as props}]
  (let [[box dimensions] (use-dimensions)
        [{:keys [top left]} set-anchor!] (hooks/use-state [-10000 -10000])]
    (letfn [(resize-handler [] (set-anchor! (calculate-position dimensions)))] 
      (hooks/use-effect
        :once
        (.addEventListener js/window "resize" resize-handler)
        (resize-handler)
        (fn []
          (.removeEventListener js/window "resize" resize-handler)))
      (hooks/use-effect
        [@box]
        (resize-handler)))
    (d/div 
      {:ref #(reset! box %)
       :class (cond-> ["modal-box" "animated" "fadeIn"]
                (vector? class) (concat class)
                (string? class) (conj class))
       :style {:position "fixed"
               :top top
               :left left}}
      (c/children props))))

(defnc ModalBody
  [{:keys [children
           class]}]
  (d/div
    {:class (clojure.string/join 
              " " 
              (concat ["modal-body"] (when (some? class) (if (seq? class) class [class]))))}
    children))

(comment
  (.log js/console ModalBody))

(defstyled background
  "div"
  {:position "fixed"
   :top "0"
   :left "0"
   :right "0"
   :bottom "0"
   :z-index "900"
   :transition "all 0.5s ease-in-out"}
  (fn [{:keys [theme]}]
    (let [color (case theme
                  "rgba(0, 0, 0, 0.68)")]
      {:background-color color})))


(defnc CloseButton
  [props]
  ($ icon/clear {& props}))

(defstyled close-button 
  CloseButton
  {:font-size "20"
   :cursor "pointer"
   :transition "color .2s ease-in"
   :width 10
   :path {:cursor "pointer"}}
  --themed)

(defstyled heading
  "div"
  {:display "flex"
   :flex-direction "row"
   :align-items "center"
   :justify-content "space-between"
   :font-weight "600"
   :margin "0 0 15px"
   :font-size "16"
   :position "relative"
   :min-height 40
   "h1 h2" {:margin-top 15}
   :span:first-of-type {:font-weight "600"}})

(defstyled free-box
  "div"
  {:display "flex"
   :flex-direction "column"
   :justify-content "space-between"
   :background-clip "padding-box"
   :max-height "calc(100% - 100px)"
   :max-width "calc(100% - 30px)"
   :z-index "990"
   :position "fixed"
   :right "auto"
   :bottom "auto"
   :padding "20px 30px 30px"}
  ;;
  (fn [{:keys [theme]}]
    (case theme
      {:background-color "white"
       :border (str "10px solid #ffffffb0")
       :border-radius 2
       :box-shadow "0px 0px 9px -1px #353535"}))
  (fn [{:keys [width height]}]
    (cond-> nil
      width (assoc :width width)
      height (assoc :height height))))

(defstyled box
  "div"
  {:display "flex"
   :height "60%"
   :flex-direction "column"
   :justify-content "space-between"
   :background-clip "padding-box"
   :max-height "calc(100% - 100px)"
   :z-index "990"
   :position "fixed"
   :right "auto"
   :bottom "auto"
   :padding "20px 30px 30px"
   :width 600}
  ;;
  (fn [{:keys [theme]}]
    (case theme
      {:background-color "white"
       :border (str "10px solid #ffffffb0")
       :border-radius 2
       :box-shadow "0px 0px 9px -1px #353535"}))
  ;;
  (fn [{:keys [shape]}]
    (cond->
      {:height "60%"
       :width 600}
      (:narrow shape) (assoc :width 400)
      (:short shape) (assoc :height 150))))


(defnc Body
  [{:keys [className] :as props} _ref]
  {:wrap [(react/forwardRef)]}
  (let [body (hooks/use-ref nil)
        [{:keys [width height]} set-size!] (hooks/use-state nil)]
    (hooks/use-effect
      [@body]
      (when @body
        (letfn [(reset [[entry]]
                  (let [content-rect (.-contentRect entry)]
                    (set-size!
                      {:width (round-number (.-width content-rect) 1 :floor)
                       :height (round-number (.-height content-rect) 1 :floor)})))]
          (let [observer (js/ResizeObserver. reset)]
            (.observe observer @body)
            (fn [] (.disconnect observer))))))
    (d/div
      {:className className
       :ref #(reset! body %)}
      ($ interactions/simplebar
         {:className "content"
          :scrollableNodeProps #js {:ref #(when _ref (reset! _ref %))}
          :style #js {:height height
                      :width width}}
        (c/children props)))))

(defstyled body Body
  {:position "relative"
   :font-size "14"
   :display "flex"
   :flex-grow "2"
   :flex-direction "column"
   :justify-content "flex-start"
   :overflow "auto"
   :-ms-overflow-style "none"
   :scrollbar-width "none"
   "&::-webkit-scrollbar" {:display "none"}})

(defstyled footer
  "div"
  {:margin-top 20
   :margin-bottom 4
   :display "flex"
   :min-height 30
   :justify-content "flex-end"
   :button {:margin "0 5px"}})




(defnc FeedbackFooter
  [{:keys [error warning className] :as props}]
  (d/div
    {:className className}
    (d/div 
      {:class (cond-> ["left"]
                ;;
                (some? error) 
                (conj "error")
                ;;
                (and (nil? error) (some? warning))
                (conj "warning"))}
      (when (some? error) 
        (<>
          ($ icon/warning {:size "2x"})
          (d/span error)))
      (when (and (nil? error) (some? warning))
        (<>
          ($ icon/warning)
          (d/span warning))))
    (d/div
      {:className "right"}
      (c/children props))))

(defstyled feedback-footer
  FeedbackFooter
  {:display "flex"
   :justify-content "space-between"
   :align-items "center"
   :margin-top 20
   :min-height 30
   ".left" 
   {:display "flex"
    :align-items "center"
    :cursor "default"
    :pointer-events "none"
    :path {:cursor "default"}}
   ".right" 
   {:display "flex"
    :justify-content "flex-end"
    :aling-items "center"
    :button {:margin "0 5px"}}}
  --themed)


(defhook use-position
  ([] (use-position true))
  ([opened?]
   (let [box (hooks/use-ref nil)
         [position set-position!] (hooks/use-state {:top -10000
                                                    :left -10000})]
     (letfn [(resize-handler [] (set-position! (calculate-position @box)))] 
       (hooks/use-effect
         :once
         (.addEventListener js/window "resize" resize-handler)
         (resize-handler)
         (fn []
           (.removeEventListener js/window "resize" resize-handler)))
       (hooks/use-effect
         [opened?]
         (when opened? (resize-handler))))
     [box position])))

(defn stop-propagation [e] 
  ; (log/debugf
  ;   :msg "Caught event in modal window"
  ;   :target (.-target e)
  ;   :type (.-type e))
  (.stopPropagation e))

(def event-stop-handlers
  (zipmap
    [:on-click :on-double-click :on-mouse-down :on-mouse-up :on-wheel 
     :on-drag-end :on-drag-start :on-drag-exit :on-mouse-leave 
     :on-mouse-enter :on-mouse-out :on-mouse-over]
    (repeat stop-propagation)))

(defnc Modal
  [props]
  (let [[visible? set-visible!] (hooks/use-state false)] 
    (hooks/use-effect
      :once
      (set-visible! true))
    (rdom/createPortal
      (d/div
        {:key (:key props)
         :style {:visibility (if visible? "visible" "hidden")}}
        (c/children props))
      (.-body js/document))))
