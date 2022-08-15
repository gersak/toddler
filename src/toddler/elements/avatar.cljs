(ns toddler.elements.avatar
  (:require-macros 
    [cljs.core.async.macros :refer [go]])
  (:require 
    ["react" :as react]
    [cljs.core.async :as async]
    [helix.styled-components :refer [defstyled --themed]]
    [helix.core 
     :refer [defnc $]]
    [helix.dom :as d]
    [helix.hooks :as hooks]
    ["react-avatar-editor" :as AvatarEditor]
    [toddler.interactions :as interactions]
    goog.object))


(def small 20)
(def medium 36)
(def large 144)

(defnc AvatarDropzone
  [{:keys [on-upload src className]} _ref]
  {:wrap [(react/forwardRef)]}
  (let [[zoom set-zoom!] (hooks/use-state 5)]
    (d/div 
      {:className className}
      (d/div
        {:className "editor"}
        (d/div
          {:className "image"}
          ($ AvatarEditor
             {:id "avatar-large"
              :ref _ref
              :className "picture"
              :scale (+ 1 (* (- zoom 5) 0.1))
              :image src
              :width large
              :height large
              :border 0}))
        (d/div
          {:className "zoom"}
          ($ interactions/slider 
             {:value zoom
              :className "zoom"
              :min 0
              :max 30
              :width 150
              :onChange (fn [e] 
                          (when src 
                            (set-zoom! (.. e -target -value))))})))
      (d/label
        {:className "upload-button"}
        (d/input
          {:style {:display "none"}
           :className "avatar-upload"
           :accept ".jpg, .png, .jpeg, .gif, .bmp, .tif, .tiff"
           :value "" 
           :type "file"
           :on-change (fn [e]
                        (let [files (.. e -target -files)
                              file (goog.object/get files 0)
                              file-reader (js/FileReader.)
                              result (async/chan)]
                          (goog.object/set
                            file-reader
                            "onloadend"
                            (fn [e]
                              (when (= 2 (.-readyState (.-target e)))
                                (let [res (.-result (.-target e))]
                                  (async/put! result res)))))
                          (.readAsDataURL file-reader file)
                          (when (fn? on-upload) 
                            (go 
                              (let [avatar (async/<! result)]
                                (on-upload avatar))))))})
        "Upload avatar"))))


(defstyled avatar-dropzone AvatarDropzone
  {:display "flex"
   :flex-direction "column"
   ".editor"
   {:display "flex"
    :flex-direction "column"
    :align-items "center"
    :justify-content "center"
    :width 145
    :margin-right 15
    :canvas {:margin-bottom 15}
    ".image" {:display "flex" :justify-content "center"}
    ".zoom" {:display "flex" :justify-content "center"}}
   :label
   {:display "flex"
    :justify-content "center"
    :right 10
    :margin 9
    :border "1px solid"
    :padding 3
    :border-radius 3
    :cursor "pointer"
    :align-self "center"}}
  --themed)


(defnc AvatarPicture
  [{:keys [path]}]
  (d/div {:class "eywa-avatar"}
    (d/div {:class "eywa-avatar-picture-wrapper"}
           (d/img {:class "eywa-avatar" :src path}))))
