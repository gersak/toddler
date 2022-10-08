(ns toddler.elements.avatar
  (:require
    [helix.core :refer [defnc defhook create-context]]
    [helix.hooks :as hooks]
    [helix.dom :as d]))


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
