(ns toddler.md
  (:require
   [clojure.core.async :as async]
   [helix.core :refer [defnc $]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.showcase.common :refer [$info]]
   ["markdown-it" :as markdownit]
   ["markdown-it-anchor" :as anchor]
   ["markdown-it-emoji"
    :refer [full]
    :rename {full emoji}]
   ["highlight.js" :as hljs]))

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

(defnc show
  [{:keys [content class className] :as props}]
  (let [editor (hooks/use-ref nil)]
    (hooks/use-effect
      [content]
      (when (and content @editor)
        (set! (.-innerHTML @editor) (.render md content))))
    (d/div
     {:ref #(reset! editor %)
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
             (.err js/console (js/Error (str "Failed to fetch: " url))))))
        (.catch
         (fn [err]
           (.err js/console (str "Failed fetching file: " url) err))))
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
  [{:keys [url interval]
    :or {interval 1000}
    :as props}]
  (let [[content set-content!] (hooks/use-state nil)]
    (hooks/use-effect
      [url interval]
      (let [close (async/chan)]
        (async/go-loop []
          (let [content (async/<! (fetch url))]
            (when (string? content) (set-content! content)))
          (async/alt!
            close
            ([_] (.log js/console (str "Removing watch for URL" url)))
            ;;
            (async/timeout interval)
            ([_] (when (pos? interval) (recur)))))
        (fn []
          (async/close! close))))
    ($ show {:content content & (dissoc props :url)})))
