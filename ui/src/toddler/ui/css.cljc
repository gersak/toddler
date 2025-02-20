(ns toddler.ui.css
  (:require
   [shadow.css :refer [css]]))

(def $store
  (css
   {:color "var(--notification-text)"}
   ["& .notification-wrapper" :py-1]
   ["& .notification"
    :flex
    :rounded-md
    :grow
    :py-3
    :px-4
    :relative
    :border
    :border-normal
    :bg-normal+
    :shadow-lg
    :items-center
    {:z-index "200"
     :transition "color .3s ease-in-out"}]
    ;;
   ["& .notification .icon"
    :flex
    :items-center
    {:order 2}]
   ["& .notification pre" :word-break :whitespace-pre-wrap]
    ;;
   ["& .notification .content"
    :flex :items-center
    :mr-2
    :grow
    {:order 2}
    :text-xxs
    :font-medium]
    ;;
   ["& .notification .close"
    :absolute
    :top-3
    :right-3
    :flex
    :items-center
    :ml-3 :pl-3
    {:order 3
     :opacity "0.5"
     :justify-self "flex-end"
     :transition "color .3s ease-in-out"}
    :text-sm]
   ["& .notification" {:background-color "var(--notification-neutral)"}]

   ["& .notification .close svg" :w-4 :h-4]
    ;;
   ["& .notification .close:hover" :cursor-pointer {:opacity "1"}]
    ;;
   ["& .notification.negative" {:background-color "var(--notification-negative)"
                                :border-color  "var(--border-negative)"}]
   ["& .notification.positive" {:background-color "var(--notification-positive)"
                                :border-color "var(--border-positive)"}]
   ["& .notification.warning" {:background-color "var(--notification-warn)"
                               :border-color "var(--border-warning)"}]))

(def $md
  (css :mt-4 :mb-24 :text-sm
       :px-4
       ["& .code" :mt-2]
       ["& h1,& h2,& h3,& h4" :uppercase]
       ["& h3" :mt-4]
       ["& h2" :mt-12]
       ["& h4" :mt-4]
       ["& p" :mt-2]
       ["& b, & strong" :font-semibold]
       ["& br" {:height "8px"}]
       ["& ul" :mt-2 :ml-4 :border {:list-style-type "disc" :border "none"}]
       ["& ul li" :text-xs]
       ["& pre > code" :rounded-lg :my-4 {:line-height "1.5"}]
       ["& li > code" :rounded-lg :my-4 {:line-height "1.5"}]
       ["& p > code" :py-1 :px-2 :rounded-md :text-xxs :bg-normal- :font-semibold]
       ["& li > code" :py-1 :px-2 :rounded-md :text-xxs :bg-normal- :font-semibold]
       ["& .table-container" :border :my-6 :p-2 :rounded-lg :bg-normal+ :border-normal+]
       ["& table tr" :h-6 :text-xxs]
       ["& a" {:color "var(--link-color)" :font-weight "600"}]
       ["& .hljs" :bg-normal+]
        ; ["& table thead tr"]
       ["& table tbody" :mt-2 :p-1]))
