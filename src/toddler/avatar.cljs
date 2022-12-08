(ns toddler.avatar
  (:require
    [goog.object]
    [toddler.ui :as ui]
    [vura.core :refer [round-number]]
    [clojure.core.async :as async]
    [helix.core :refer [defnc $ <> create-context defhook provider]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    [helix.konva :as konva]
    [helix.children :as c]
    [helix.image :refer [use-image]]))


(def ^:dynamic *avatar-root* (create-context ""))


(defhook use-avatar-root
  []
  (hooks/use-context *avatar-root*))


(defnc Image
  [{:keys [onSelect onChange ]
    {:keys [image] :as avatar} :avatar}]
  (let [shape-ref (hooks/use-ref nil)]
    (when image
      (<>
       (konva/Image
        {:& avatar
         :onClick (fn [^js e] (onSelect (.. e -target)))
         :ref #(reset! shape-ref %)
         :draggable true
         :onDragMove (fn [^js e]
                       (let [node ^js @shape-ref]
                         (onChange
                          {:width (.width node)
                           :height (.height node)
                           :x (.x (.-target e))
                           :y (.y (.-target e))})))})))))


(defn download-uri [^js uri, name, mime]
  (let [^js document js/document
        ^js body (.-body document)
        ^js link (doto (.createElement document "a")
                   (goog.object/set "download" name)
                   (goog.object/set "href" uri)
                   (goog.object/set "type" mime))]
    (.appendChild body link)
    (.click link)
    (.removeChild body link)))



(def ^:dynamic ^js *stage* (create-context))


(defhook use-stage []
  (hooks/use-context *stage*))


(defnc EditorStage [{:keys [image className size]
               :or {size 144}
               :as props} _ref]
  {:wrap [(ui/forward-ref)]}
  (let [half-size (hooks/use-memo
                    [size]
                    (/ size 2))
        [avatar set-avatar!] (hooks/use-state nil)
        local-ref (hooks/use-ref nil)
        stage-ref (hooks/use-memo
                    :once
                    (or _ref (fn [el] (reset! local-ref el))))
        [layer-zoom set-layer-zoom] (hooks/use-state 0.5)
        frame-props (hooks/use-memo
                      [size]
                      {:x (* -1 0.1 size)
                       :y (* -1 0.1 size)
                       :visible true
                       :cornerRadius (* size 0.2)
                       :listening false
                       :width (* size 1.2)
                       :height (* size 1.2)
                       :strokeWidth (* size 0.3)
                       :stroke "#000000bb"
                       :style {:z-index "10"}})]
    (hooks/use-effect
      [image]
      (set-layer-zoom 0.5)
      (when image
        (set-avatar!
          {:image image
           :offsetX (if (= image js/undefined) 0
                      (/ (.-width image) 2))
           :offsetY (if (= image js/undefined) 0
                      (/ (.-height image) 2))
           :rotation 0
           :width (.-width image)
           :height (.-height image)
           :x (if (= image js/undefined) 0
                half-size)
           :y (if (= image js/undefined) 0
                half-size)})))
    (<>
      (d/div
        {:className className}
        (konva/Stage
          {:width size
           :height size 
           :ref stage-ref
           :onWheel (fn [^js e]
                      (let [evt (.-evt e)]
                        (.preventDefault evt)
                        (.stopPropagation evt)))}
          (konva/Layer
            {:x half-size
             :y half-size
             :offsetX half-size
             :offsetY half-size 
             :scaleX layer-zoom
             :scaleY layer-zoom
             :onWheel (fn [^js e]
                        (let [scale-by 1.02
                              old-scale layer-zoom
                              new-scale (if (< (.-deltaY (.-evt e)) 0)
                                          (* old-scale scale-by)
                                          (/ old-scale scale-by))]
                          (set-layer-zoom new-scale)))}
            ($ Image
               {:avatar avatar
                :onChange (fn [delta] (set-avatar! merge delta))}))
          (konva/Layer
            (konva/Rect
              {& frame-props}))))
      (provider
        {:context *stage*
         :value stage-ref}
        (c/children props)))))


(defnc Editor [{:keys [size]
                :or {size 250}}]
  (let [stage (hooks/use-ref nil)
        [file set-file] (hooks/use-state nil)
        [image] (use-image file "anonymous")]
    ($ EditorStage
       {:ref #(reset! stage %)
        :image image
        :size size}
       (<>
         ($ ui/button
            {:onClick (fn []
                        (let [[layer1 layer2] (.-children @stage)
                              [_ ^js transformer] ^js (.-children layer1)
                              frame ^js (.-children layer2)]
                          (doseq [^js el frame] (.hide el))
                          (when transformer (.hide transformer))
                          (download-uri (.toDataURL @stage), "user-avatar.png", "image/png")
                          (doseq [^js el frame] (.show el))
                          (when transformer (.show transformer))))}
            (str "Download/log URI"))
         (d/input {:id "input"
                   :name "input"
                   :type "file"
                   :accept ".png, .jpg, .jpeg, .svg"
                   :onChange (fn [e]
                               (cond (not (nil? (-> e .-target .-files)))
                                 (let [URL (.-URL js/window)
                                       url (.createObjectURL URL (first (.. e -target -files)))
                                       img (doto (.createElement js/document "img")
                                             (goog.object/set "src" url))]
                                   (set-file (.-src img)))))
                   :style {:color "teal"}})))))


(defnc GeneratorStage
  [{:keys [className size squares color background style]
    :or {size 500
         squares 16
         color "gray"
         background "white"}
    :as props} _ref]
  {:wrap [(ui/forward-ref)]}
  (let [squares (hooks/use-memo
                  [size squares color background]
                  (let [squares (round-number (/ squares 2) 1 :floor)
                        square-size (/ size squares)]
                    (reduce
                      concat
                      (for [x (range squares) y (range squares)
                            :let [draw? (rand-nth [true false])]]
                        [{:key (str [x y])
                          :x (* x square-size)
                          :y (* y square-size)
                          :fill (if draw? color background)
                          :width square-size
                          :height square-size}
                         {:key (str [(+ squares (- squares x)) y])
                          :x (- size square-size (* x square-size))
                          :y (* y square-size)
                          :fill (if draw? color background)
                          :width square-size
                          :height square-size}]))))]
    (<>
      (d/div
        {:className className
         :style style}
        (konva/Stage
          {:width size
           :height size
           :ref _ref}
          (konva/Layer
            (map
              (fn [square]
                (konva/Rect {& square}))
              squares))))
      (c/children props))))


; (defstyled hidden-generator GeneratorStage
;   {:visibility "hidden"
;    :position "fixed"
;    :top 0
;    :left 0})


(def ^:dynamic ^js *generator-queue* (create-context))

(defhook use-generator-queue [] (hooks/use-context *generator-queue*))


(defnc Generator
  [{:keys [palette className]
    :or {palette ["#FB8B24"
                  "#EA4746"
                  "#E22557"
                  "#AE0366"
                  "#820263"
                  "#560D42"
                  "#175F4C"
                  "#04A777"
                  "#46A6C9"
                  "#385F75"
                  "#313B4B"
                  "#613C69"
                  "#913D86"
                  "#F03FC1"]}
    :as props}]
  (let [[color set-color!] (hooks/use-state (rand-nth palette))
        queue (hooks/use-memo :once (async/chan 500))
        stage (hooks/use-ref nil)
        requests (hooks/use-ref nil)]
    (hooks/use-effect
      :once
      (let [close? (async/chan)]
        (async/go-loop []
          (async/alt!
            ;;
            close?
            ([_] (.log js/console "Closing generator"))
            ;;
            queue
            ([request-channel]
             (if (empty? @requests)
               (do
                 (reset! requests [request-channel])
                 (set-color! (rand-nth palette)))
               (swap! requests (fnil conj []) request-channel))
             (recur))))
        (fn [] (async/close! close?))))
    (hooks/use-layout-effect
      :always
      (let [[request] @requests]
        (when (and @stage request)
          (let [img (.toDataURL @stage)]
            (async/put! request img)
            (swap! requests (comp vec rest))
            (when (not-empty @requests)
              (async/go (set-color! (rand-nth (remove #{color} palette)))))))))
    (provider
      {:context *generator-queue*
       :value queue}
      ($ GeneratorStage
         {:color color 
          :className className
          :background "white"
          :ref stage})
      (c/children props))))



(defnc Avatar
  [{:keys [className onGenerate style]
    value :avatar}]
  (let [root (use-avatar-root)
        [avatar set-avatar!] (hooks/use-state nil)
        generator-queue (use-generator-queue)]
    (hooks/use-memo
      [value]
      (if (some? value)
        (if (re-find #"^data:image" value)
          (when-not (= value avatar) (set-avatar! avatar))
          (let [url (str root value)]
            (when-not (= avatar url) (set-avatar! url))))
        (when (some? generator-queue)
          (async/go
            ;; create generated promise
            (let [generated (async/promise-chan)]
              ;; send promise to generator queue
              (async/>! generator-queue generated)
              ;; then wait for generated result
              (let [avatar (async/<! generated)]
                ;; and handle stuff
                (when (fn? onGenerate) (onGenerate avatar))
                (set-avatar! avatar)))))))
    (d/img
      {:className className
       :style style
       :src avatar})))
