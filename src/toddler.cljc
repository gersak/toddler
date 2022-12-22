(ns toddler
  (:require
    [shadow.css :refer [css]]))


(def $tag
  (css
    :rounded-sm
    :px-2
    :py-1
    :m-1
    :flex
    :justify-center
    :items-center
    :text-neutral-600
    ["& svg" :ml-2 :pr-1]
    ["& .remove"
     :cursor-pointer
     :flex
     :items-center
     :justify-center
     {:transition "color .2s ease-in"}]
    ;; Colors
    :text-neutral-100
    ; :bg-gray-600
    ; :font-semibold
    {:background-color "#344a6a"}
    ["& .remove:hover" :text-rose-400]
    ["& .remove" {:color "#647288"}]
    ["& .remove path" :cursor-pointer]
    ;;
    ["&.positive"
     :text-neutral-600
     {:background-color "#c4ed7c"}]
    ["&.positive .remove:hover" :text-black]
    ["&.positive .remove" :text-cyan-600]
    ;;
    ["&.negative"
     :text-neutral-50
     :bg-rose-400
     {:background-color "#d64242"}]
    ["&.negative .remove:hover" :text-black]
    ["&.negative .remove" {:color "#a10303"}]))


(def $table-default
  (css
    :flex
    :column
    :grow
    :text-neutral-600
    :border
    :border-solid
    :rounded-md
    :shadow-lg
    {:background-color "#e2f1fc"
     :border-color "#8daeca"}
    ["& .trow"
     :my-1
     :border-b
     :border-transparent
     {:min-height "2em"
      :transition "all .5s ease-in-out"}]
    ["& .trow:hover, & .trow:focus-within" :border-b
     {:border-color "#69b5f3"
      :background-color "#d0e9fb"}]))


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
