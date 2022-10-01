(ns toddler.ui.provider
  (:require
    [helix.core :refer [provider defnc $]]
    [helix.children :as c]
    [toddler.ui :as ui]))


(defnc DataFields
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


(defnc AppFields
  [{:keys [components] :as props}]
  (provider
    {:context ui/$search-field
     :value (components :field/search)}
    (provider
      {:context ui/$user-field
       :value (components :field/user)}
      (provider
        {:context ui/$group-field
         :value (components :field/group)}
        (provider
          {:context ui/$user-multiselect-field
           :value (components :field/user-multiselect)}
          (provider
            {:context ui/$group-field
             :value (components :field/group-multiselect)}
            (c/children props)))))))


(defnc Layout
  [{:keys [components] :as props}]
  (provider
    {:context ui/$row
     :value (components :row)}
    (provider
      {:context ui/$column
       :value (components :column)}
      (provider
        {:context ui/$card
         :value (components :card)}
        (c/children props)))))


(defnc App
  [{:keys [components] :as props}]
  (provider
    {:context ui/$avatar
     :value (components :avatar)}
    (c/children props)))


(defnc UI
  [{:keys [components] :as props}]
  ($ App
    {:components components}
    ($ Layout
       {:components components}
       ($ AppFields
          {:components components}
          ($ DataFields
             {& props})))))
