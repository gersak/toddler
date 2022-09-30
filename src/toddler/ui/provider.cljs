(ns toddler.ui.provider
  (:require
    [helix.core :refer [provider defnc $]]
    [helix.children :as c]
    [toddler.ui :as ui]))


(defnc Fields
  [{:keys [components] :as props}]
  (provider
    {:context ui/$text-field
     :value (components :field/text)}
    (provider
      {:context ui/$integer-field
       :value (components :field/integer)}
      (provider
        {:context ui/$float-field
         :value (components :field/float)}
        (provider
          {:context ui/$input-field
           :value (components :field/input)}
          (provider
            {:context ui/$dropdown-field
             :value (components :field/dropdown)}
            (provider
              {:context ui/$multiselect-field
               :value (components :field/multiselect)}
              (provider
                {:context ui/$timestamp-field
                 :value (components :field/timestamp)}
                (provider
                  {:context ui/$period-field
                   :value (components :field/period)}
                  (c/children props))))))))))

(defnc UI
  [{:keys [components] :as props}]
  ($ Fields
    {& props}))
