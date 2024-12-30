(ns toddler.avatar
  (:require
   [helix.core :refer [defnc]]
   [helix.dom :as d]))

(defnc avatar
  [{:keys [name size style]
    :or {size :small}
    :as props}]
  (let [size' (case size
                :small "2em"
                :medium "4em"
                :large "10em"
                size)
        src (str "https://api.dicebear.com/9.x/pixel-art/svg?seed=" name)]
    (d/img
     {:src src
      :style (merge
              style
              {:user-select "none"
               :min-width size'
               :width size'
               :min-height size'
               :height size'})
      & (select-keys props [:class :className :onFocus :onBlur])})))
