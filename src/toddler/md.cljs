(ns toddler.md
  (:require
   [toddler.app :as app]
   [clojure.string :as str]
   [clojure.core.async :as async]
   [helix.core :refer [defnc $ <> memo]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.core :refer [fetch]]
   [toddler.showcase.css :refer [$default]]
   [toddler.showcase]
   ["markdown-it" :as markdownit]
   ["markdown-it-anchor" :as anchor]
   ["markdown-it-emoji"
    :refer [full]
    :rename {full emoji}]
   ["highlight.js" :as hljs]))

(def md
  (let [md (->
            (markdownit
             #js {:html true
                  :highlight (fn [text lang]
                               (try
                                 (str
                                  "<pre><code class=hljs>"
                                  (.-value (.highlight hljs lang text))
                                  "</code></pre>")
                                 (catch js/Error ex
                                   (.error js/console ex)
                                   (str ""))))})
            ; (.use anchor)
            (.use emoji))]
    (set! (.. md -renderer -rules -heading_open)
          (fn [tokens idx _ _ _]
            (let [level (.-tag (get tokens idx))]
              (if (#{"h2" "h1"} level)
                (let [id (when-some [_id (get tokens (inc idx))]
                           (some-> (not-empty (.-content _id))
                                   (str/lower-case)
                                   (str/replace #"\s+" "-")))]
                  (str "</section><section id=\"" id "\"><" level ">"))
                level))))
    (set! (.. md -renderer -rules -heading_close)
          (fn [tokens idx _ _ _]
            (let [level (.-tag (get tokens idx))]
              (str "</" level ">"))))
    md))

(.log js/console "MD: " md)

(defn check-diff
  [a b]
  (= (:content a) (:content b)))

(defnc show
  {:wrap [(memo check-diff)]}
  [{:keys [content class className]}]
  (let [editor (hooks/use-ref nil)
        text (hooks/use-memo
               [content]
               (when content
                 (.render md content)))
        observer (hooks/use-memo
                   :once
                   (js/IntersectionObserver.
                    (fn [entries _observer]
                      (doseq [entry entries]
                        (when (.-isIntersecting entry)
                          (async/put!
                           app/signal-channel
                           {:topic ::intersection
                            :id (.. entry -target -id)}))))
                    #js {:root nil
                         :rootmargin "0"}))]
    (hooks/use-effect
      :always
      (let [sections (.querySelectorAll js/document "section")]
        (.log js/console "HEAD")
        (doseq [section sections]
          (.observe observer section))
        (fn []
          (doseq [section sections]
            (.unobserve observer section)))))
    (d/div
     {:ref #(reset! editor %)
      :dangerouslySetInnerHTML #js {:__html text}
      :class (cond-> [$default "toddler-markdown"]
               (string? className) (conj className)
               (string? class) (conj class)
               (sequential? class) (into class))})))

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
            ([_] (.log js/console (str "Removing watch for URL: " url)))
            ;;
            (async/timeout interval)
            ([_] (when (pos? interval) (recur)))))
        (fn []
          (async/close! close))))
    ($ show {:content content & (dissoc props :url)})))
