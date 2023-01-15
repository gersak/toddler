(ns toddler
  (:require
    [shadow.css :refer [css]]))


(def aliases
  {::menu-link {:color "#6f9695" :text-decoration "none"}
   ::menu-link-selected {:color "#67fda7"}
   ::background {:color "#003447"}
   ::dropdown-bg {:color "white"} 
   ::dropdown-simplebar (css
                          ["& .simplebar-scrollbar:before"
                           :bg-gray-100
                           :pointer-events-none
                           {:max-height "400px"}])
   :select-none {:user-select "none"}
   ::dropdown-option (css
                       :flex
                       :justify-start
                       :items-center
                       :cursor-pointer
                       :text-gray-500
                       :rounded-sm
                       :bg-white
                       {:transition "color .2s ease-in,background-color .2s ease-in"
                        :padding "4px 6px 4px 4px"}
                       [:hover :text-neutral-600
                        {:background-color "#e2f1fc"}]
                       ["&:last-child" {:border-bottom "none"}])})
