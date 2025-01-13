(ns toddler.md
  (:require
   [taoensso.telemere :as t]
   [clojure.core.async :as async]
   [helix.core :refer [defnc $ <> memo]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [shadow.css :refer [css]]
   ["markdown-it" :as markdownit]
   ["markdown-it-anchor" :as anchor]
   ["markdown-it-emoji"
    :refer [full]
    :rename {full emoji}]
   ["highlight.js" :as hljs]))

(def $info
  (css :mt-4 :text-sm
       ["& .code" :mt-2]
       ["& h1,& h2,& h3,& h4" :uppercase :mt-4]
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
    (d/div
     {:ref #(reset! editor %)
      :dangerouslySetInnerHTML #js {:__html text}
      :class (cond-> [$info "toddler-markdown"]
               (string? className) (conj className)
               (string? class) (conj class)
               (sequential? class) (into class))
      & (dissoc props :class :className :content)})))

(defn fetch [url]
  (let [result (async/promise-chan)]
    (-> (js/fetch url)
        (.then
         (fn [response]
           (if (.-ok response)
             (-> (.text response)
                 (.then (fn [text] (async/put! result text)))
                 (.catch (fn [err] (async/put! result err))))
             (.error js/console (js/Error (str "Failed to fetch: " url))))))
        (.catch
         (fn [err]
           (.error js/console (str "Failed fetching file: " url) err))))
    result))

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
    :or {interval 1000}
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
