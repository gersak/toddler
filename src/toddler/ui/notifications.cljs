(ns toddler.ui.notifications
  {:shadow.css/include
   ["css/toddler_notifications.css"]}
  (:require
    clojure.string
    goog.string.format
    shadow.css
    [helix.core :refer [$]]
    [toddler.notifications :refer [render-notification]]
    [helix.dom :as d]
    ["toddler-icons" :as icon]))



(defmethod render-notification ::error
  [{:keys [visible? hideable? message idx dispatch hidding? adding?]}]
  (d/div 
    {:key idx
     :class (cond-> ["notification" "error"]
              (and visible? adding? (not hidding?)) 
              (conj "new")
              ;;
              (and visible? (not adding?) (not hidding?)) 
              (conj "show") 
              ;;
              hidding? (conj "hide"))}
    (d/div 
      {:class "close"
       :style {:visibility 
               (if (or
                     (not hideable?)
                     (not visible?))
                 "hidden"
                 "visible")}}
      (d/div
        "close")
      ($ icon/close
         {:onClick #(dispatch
                      {:type :notification/hide
                       :idx idx})}))
    (d/div 
      {:class "type"}
      ($ icon/error))
    (d/div 
      {:class "content"} 
      (d/div {:class "message"} (d/span message)))))


(defmethod render-notification ::warning 
  [{:keys [visible? hidding? hideable? message idx dispatch adding?]}]
  (d/div 
    {:key idx
     :class (clojure.string/join
              " " 
              (cond-> ["notification" "warning"]
                (and visible? adding? (not hidding?)) 
                (conj "new")
                ;;
                (and visible? (not adding?) (not hidding?)) 
                (conj "show") 
                ;;
                hidding? (conj "hide")))}
    (d/div 
      {:class "close"
       :style {:visibility 
               (if (or
                     (not hideable?)
                     (not visible?))
                 "hidden"
                 "visible")}}
      ($ icon/close
         {:onClick #(dispatch
                      {:type :notification/hide
                       :idx idx})}))
    (d/div 
      {:class "type"}
      ($ icon/warning))
    (d/div 
      {:class "content"} 
      (d/div {:class "message"} (d/span message)))))


(defmethod render-notification ::success 
  [{:keys [visible? hideable? hidding? message idx dispatch adding?]}]
  (d/div 
    {:key idx
     :class (clojure.string/join 
              " " 
              (cond-> ["notification" "success"]
                (and visible? adding? (not hidding?)) 
                (conj "new")
                ;;
                (and visible? (not adding?) (not hidding?)) 
                (conj "show") 
                ;;
                hidding? (conj "hide")))}
    (d/div 
      {:class "close"
       :style {:visibility 
               (if (or
                     (not hideable?)
                     (not visible?))
                 "hidden"
                 "visible")}}
      ($ icon/close
        {:onClick #(dispatch
                     {:type :notification/hide
                      :idx idx})}))
    (d/div 
      {:class "type"}
      ($ icon/success))
    (d/div 
      {:class "content"} 
      (d/div 
        {:class "message"}
        (d/span message)))))
