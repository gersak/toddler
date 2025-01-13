(ns toddler.showcase.common
  (:require
   [helix.core :refer [defhook]]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]))

(def $info
  (css :mt-4 :text-sm
       ["& .code" :mt-2]
       ["& h4" :uppercase]
       ["& p" :mt-2]
       ["& b, & strong" :font-semibold]
       ["& br" {:height "8px"}]
       ["& ul" :mt-2 :ml-4 :border {:list-style-type "disc" :border "none"}]
       ["& ul li" :text-xs]
       ["& pre code" :rounded-lg {:line-height "1.3"}]
       ["& .table-container" :border :my-6 :p-2 :rounded-lg {:background-color "var(--background-lighter)"}]
       ["& table tr" :h-6 :text-xxs]
       ["& .hljs" {:background-color "var(--background-lighter)"}]
       ; ["& table thead tr"]
       ["& table tbody" :mt-2 :p-1]))

(defn refresh-highlight []
  (.log js/console "Calling refresh")
  (js/hljs.highlightAll))

(defhook use-code-refresh
  []
  (hooks/use-effect
    :always
    (refresh-highlight)))
