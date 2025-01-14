(ns toddler.md
  (:require
   [taoensso.telemere :as t]
   [clojure.core.async :as async]
   [helix.core :refer [defnc $ <> memo]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.core :refer [fetch]]
   [toddler.showcase.css :refer [$default]]
   ["markdown-it" :as markdownit]
   ["markdown-it-anchor" :as anchor]
   ["markdown-it-emoji"
    :refer [full]
    :rename {full emoji}]
   ["highlight.js" :as hljs]))

#_(def $default
    (css :mt-4 :text-sm
         ["& .code" :mt-2]
         ["& h1,& h2,& h3,& h4" :uppercase :mt-4]
         ["& p" :mt-2]
         ["& b, & strong" :font-semibold]
         ["& br" {:height "8px"}]
         ["& ul" :mt-2 :ml-4 :border {:list-style-type "disc" :border "none"}]
         ["& ul li" :text-xs]
         ["& pre > code" :rounded-lg :my-2 {:line-height "1.5"}]
         ["& p > code" :py-1 :px-2 :rounded-md :text-xxs :bg-normal- :font-semibold]
         ["& .table-container" :border :my-6 :p-2 :rounded-lg :bg-normal+ :border]
         ["& table tr" :h-6 :text-xxs]
         ["& .hljs" :bg-normal+]
       ; ["& table thead tr"]
         ["& table tbody" :mt-2 :p-1]))

(def md (->
         (markdownit
          #js {:html true
               :highlight (fn [text lang]
                            (if (and lang (.getLanguage hljs lang))
                              (try
                                (str
                                 "<pre><code class=hljs>"
                                 (.-value (.highlightAuto hljs text))
                                 "</code></pre>")
                                (catch js/Error ex
                                  (.error js/console ex)))
                              (str "")))})
         (.use anchor)
         (.use emoji)))

(defn check-diff
  [a b]
  (= (:content a) (:content b)))

(defnc show
  {:wrap [(memo check-diff)]}
  [{:keys [content class className] :as props}]
  (let [editor (hooks/use-ref nil)
        text (hooks/use-memo
               [content]
               (when content
                 (.render md content)))
        id (hooks/use-memo
             :once
             (gensym "md_"))]
    (hooks/use-effect
      :once
      (t/log!
       {:id ::mounting
        :level :debug
        :data {:id id
               :content content
               :class class
               :className className}}))
    (t/log!
     {:id ::refreshing
      :level :debug
      :data {:id id}})
    (when text
      (d/div
       {:ref #(reset! editor %)
        :dangerouslySetInnerHTML #js {:__html text}
        :class (cond-> [$default "toddler-markdown"]
                 (string? className) (conj className)
                 (string? class) (conj class)
                 (sequential? class) (into class))
        & (dissoc props :class :className :content)}))))

(defnc from-url
  [{:keys [url] :as props}]
  (let [[content set-content!] (hooks/use-state nil)]
    (hooks/use-effect
      [url]
      (when (nil? content)
        (async/go
          (let [content (async/<! (fetch url))]
            (when (string? content) (set-content! content))))))
    ($ show {:content content & (dissoc props :url)})))

(defnc watch-url
  {:wrap [(memo #(= (:url %1) (:url %2)))]}
  [{:keys [url interval]
    :or {interval 4000}
    :as props}]
  (let [[content set-content!] (hooks/use-state nil)]
    (hooks/use-effect
      [url interval]
      (let [close (async/chan)]
        (async/go-loop []
          (let [_content (async/<! (fetch url))]
            (when (and (string? _content)
                       (not= _content content))
              (set-content! _content)))
          (async/alt!
            close
            ([_] (.log js/console (str "Removing watch for URL" url)))
            ;;
            (async/timeout interval)
            ([_] (when (pos? interval) (recur)))))
        (fn []
          (async/close! close))))
    ($ show {:content content & (dissoc props :url)})))
