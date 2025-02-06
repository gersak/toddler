(ns toddler.showcase.css
  (:require
   [shadow.css :refer [css]]))

(def $default
  (css :mt-4 :mb-24 :text-sm
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
