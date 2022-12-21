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
    ["& .remove path" :cursor-pointer]))


(def $tag-default
  (css
    :text-neutral-200
    :bg-gray-600
    ; :font-semibold
    ["& .remove:hover" :text-rose-400]
    ["& .remove" :text-gray-400]))


(def $tag-positive
  (css
    :text-neutral-600
    {:background-color "#e5e870"}
    ["& .remove:hover" :text-black]
    ["& .remove" :text-cyan-600]))


(def $tag-negative
  (css
    :text-neutral-50
    :bg-rose-400
    {:background-color "#d64242"}
    ["& .remove:hover" :text-black]
    ["& .remove" {:color "#a10303"}]))


(def aliases
  {::menu-link {:color "#6f9695" :text-decoration "none"}
   ::menu-link-selected {:color "#67fda7"}
   ::background {:color "#003447"}})
