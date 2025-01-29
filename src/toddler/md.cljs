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
   [toddler.util :as util]
   [toddler.router :as router]
   [toddler.head :as head]
   [toddler.md.context :as md.context]
   [shadow.css :refer [css]]
   ["markdown-it" :as markdownit]
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
                (str "<" level ">")))))
    (set! (.. md -renderer -rules -heading_close)
          (fn [tokens idx _ _ _]
            (let [level (.-tag (get tokens idx))]
              (str "</" level ">"))))
    md))

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
        {:keys [hash]} (router/use-location)
        scroll (hooks/use-ref nil)
        theme (app/use-theme)
        ; dark-url "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/tokyo-night-dark.min.css"
        dark-url "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/base16/tomorrow-night.min.css"
        light-url "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/base16/atelier-lakeside-light.min.css"]
    (hooks/use-effect
      [theme]
      (letfn [(fetch-dark []
                (head/remove
                 :link
                 {:href light-url
                  :rel "stylesheet"})
                (head/add
                 :link
                 {:href dark-url
                  :rel "stylesheet"}))
              (fetch-light []
                (head/remove
                 :link
                 {:href dark-url
                  :rel "stylesheet"})
                (head/add
                 :link
                 {:href light-url
                  :rel "stylesheet"}))]
        (case theme
          "light" (fetch-light)
          "dark" (fetch-dark)
          nil)))
    (hooks/use-effect
      :once
      (when-let [scroll-element (util/find-parent
                                 @editor
                                 (fn [^js el]
                                   (let [class (.getAttribute el "class")]
                                     (when (and el class (.includes class "simplebar-content-wrapper"))
                                       el))))]
        (letfn [(on-scroll [event]
                  (let [{:keys [sections]
                         currently-visible :visible} @scroll
                        scroll-element (.-target event)
                        scroll-dimensions (.getBoundingClientRect scroll-element)
                        scroll-height (.-height scroll-dimensions)
                        start (.-scrollTop scroll-element)
                        end (+ start scroll-height)
                        visible (set
                                 (keep
                                  (fn [^js el]
                                    (let [section-start (.-offsetTop el)
                                          section-height (.-height (.getBoundingClientRect el))
                                          section-end (+ section-start section-height)]
                                      (when (or
                                                ;; If section start is currently visible
                                             (<= start section-start end)
                                                ;; if section start isn't visible but
                                                ;; section hasn't ended jet
                                             (<= section-start start section-end)
                                                ;;
                                             (<= section-start end section-end))
                                        (.getAttribute el "id"))))
                                  sections))]
                    (when (not= currently-visible visible)
                      (swap! scroll assoc :visible visible)
                      (async/put!
                       app/signal-channel
                       {:topic ::intersection
                        :ids visible}))))]
          (.addEventListener scroll-element "scroll" on-scroll)
          (reset! scroll {:scroll-element scroll-element})
          (fn []
            (.removeEventListener scroll-element "scroll" on-scroll)))))
    (hooks/use-layout-effect
      [hash]
      (when hash
        (async/go
          (async/<! (async/timeout 500))
          (when-let [el (.getElementById js/document hash)]
            (when (.contains (:scroll-element @scroll) el)
              (let [offset-top (.-offsetTop el)
                    scroll (:scroll-element @scroll)]
                (.scrollTo scroll
                           #js {:top offset-top
                                :behavior "smooth"}))))
          (let [sections (filter
                          (fn [el]
                            (.contains (:scroll-element @scroll) el))
                          (.querySelectorAll js/document "section"))]
            (swap! scroll assoc :sections sections)))))
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
  [{:keys [url]
    :as props}]
  (let [[content set-content!] (hooks/use-state nil)
        interval (hooks/use-context md.context/refresh-period)
        base (hooks/use-context md.context/base)]
    (hooks/use-effect
      [url interval base]
      (when (or (pos? interval)
                (nil? content))
        (let [close (async/chan)
              url (str base url)]
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
            (async/close! close)))))
    ($ show {:content content & (dissoc props :url)})))
