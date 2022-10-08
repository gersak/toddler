(ns toddler.elements.avatar
  (:require
    [goog.object]
    [toddler.ui :as ui]
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

(defnc Avatar
  [{:keys [avatar className]}]
  (let [root (use-avatar-root)
        avatar' (when avatar
                  (if (re-find #"^data:image" avatar)
                    avatar
                    (str root avatar)))]
    (d/img
      {:class className
       :src avatar'})))


(defnc Image
  [{:keys [onSelect onChange ]
    {:keys [image] :as avatar} :avatar}]
  (let [shape-ref (hooks/use-ref nil)]
    (when image
      (<>
       (konva/Image
        {:& avatar
         :onClick (fn [e] (onSelect (.. e -target)))
         :ref #(reset! shape-ref %)
         :draggable true
         :onDragMove (fn [e]
                       (let [node ^js @shape-ref]
                         (onChange
                          {:width (.width node)
                           :height (.height node)
                           :x (.x (.-target e))
                           :y (.y (.-target e))})))})))))


(defn download-uri [uri, name, mime]
  (let [link (doto (.createElement js/document "a")
               (set! -download name)
               (set! -href uri)
               (set! -type mime))]
    (.appendChild (.-body js/document) link)
    (.click link)
    (.removeChild (.-body js/document) link)))



(def ^:dynamic *stage* (create-context))


(defhook use-stage []
  (hooks/use-context *stage*))


(defnc Stage [{:keys [image className size]
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
      (println "Image reloaded!")
      (.log js/console image)
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
             :onWheel (fn [e]
                        (let [scale-by 1.02
                              old-scale layer-zoom
                              new-scale (if (< (.-deltaY (.-evt e)) 0)
                                          (* old-scale scale-by)
                                          (/ old-scale scale-by))]
                          (set-layer-zoom new-scale)))}
            ($ Image
               {:avatar avatar
                :onChange (fn [delta]  (set-avatar! merge delta))}))
          (konva/Layer
            (konva/Rect
              {& frame-props}))))
      (provider
        {:context *stage*
         :value stage-ref}
        (c/children props)))))


(defnc Editor []
  (let [stage (hooks/use-ref nil)
        [file set-file] (hooks/use-state nil)
        [image] (use-image file "anonymous")]
    ($ Stage
       {:ref #(reset! stage %)
        :image image
        :size 500}
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
