(ns toddler.elements.checklist
  (:require
    clojure.set
    [helix.styled-components :refer-macros [defstyled]]
    ["@fortawesome/free-solid-svg-icons"
     :refer [faSquare
             faCheckSquare]]
    [toddler.interactions :as interactions]
    [helix.dom :as d]
    [helix.core
     :as hx
     :refer [defnc $]]))


(defnc ChecklistField 
  [{cname :name 
    value :value 
    onChange :onChange}]
  (d/div
    {:class "row"}
    (d/div 
      {:class "value"
       :onClick #(onChange (not value))}
      ($ interactions/fa
         {:icon (case value
                  true faCheckSquare
                  faSquare)
          :pull "left"}))
    (d/div 
      {:class "name"}
      cname)))


(defnc ChecklistElement [{:keys [value
                                 options
                                 multiselect?
                                 display-fn
                                 onChange
                                 className] 
                          :or {display-fn identity
                               onChange identity
                               value []}}]
  (let [value' (clojure.set/intersection
                 (set options)
                 (if multiselect? 
                   (set value)
                   #{value}))] 
    (d/div
      {:className className}
      (d/div
        {:class "list"}
        (map
          (fn [option]
            ($ ChecklistField
              {:key (display-fn option)
               :name (display-fn option)
               :value (boolean (contains? value' option))
               :onChange #(onChange
                            (if (true? %)
                              (if multiselect?
                                ((fnil conj []) value' option) 
                                option)
                              (if multiselect?
                                (vec (remove #{option} value'))
                                nil)))}))
          options)))))

(defstyled checklist ChecklistElement
  {:display "flex"
   :justify-content "center"
   ".list" 
   {:display "flex"
    :flex-direction "column"
    :flex-wrap "wrap"
    ".row" 
    {:display "flex"
     :align-content "center"
     :margin-bottom 3
     :max-width 250
     ".value" {:display "flex" 
               :justify-content "center" 
               :align-items "center"
               (str interactions/fa) {:cursor "pointer"}}}}})
