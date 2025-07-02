(ns toddler.md
  (:require
   [toddler.app :as app]
   [clojure.string :as str]
   [clojure.core.async :as async]
   [helix.core :refer [defnc $ memo provider fnc]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.ui :as ui]
   [toddler.core :as toddler :refer [fetch]]
   [toddler.util :as util]
   [toddler.router :as router]
   [toddler.md.context :as md.context]
   ["markdown-it" :as markdownit]
   ["markdown-it-emoji"
    :refer [full]
    :rename {full emoji}]
   ["markdown-it-container" :as container]
   ["highlight.js" :as hljs]))

(defn prepare-containers
  [md]
  (letfn [(render [tokens idx _ _]
            (let [token (aget tokens idx)
                  nesting? (.-nesting token)
                  info (.-info token)
                  [_ type title] (re-matches #"^(\w+)\s*(.*)$" info)
                  type (when type (str/lower-case type))]
              (if (= nesting? 1)
                (str "<div class=\"container-block " type " \">"
                     (when-not (empty? title)
                       (str "<div class=\"container-block-title\"><span class=\"icon\"></span>" title "</div>\n")))
                (str "</div>"))))]
    (reduce
     (fn [md word]
       (.use md container word #js {:render render})
       md)
     md
     ["tip" "info" "note" "warning" "danger"])))

(defn _highlight
  [text lang]
  (try
    (str
     "<div class=\"code-wrapper\">"
     "<button class=\"copy-button\">Copy</button>"
     "<pre><code class=hljs>"
     (.-value (.highlight hljs lang text))
     "</code></pre>"
     "</div>")
    (catch js/Error ex
      (.error js/console ex)
      (str ""))))

(def md
  (let [^js md (cond->
                (markdownit
                 #js {:html true
                      :highlight _highlight})
                 emoji (.use emoji)
                 container prepare-containers)]
    (set! (.. md -renderer -rules -heading_open)
          (fn [tokens idx _ _ _]
            (let [level (.-tag (get tokens idx))]
              (if (#{"h2" "h1" "h3" "h4"} level)
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

(defn wrap-base [component base]
  (fnc MD [props]
    (provider
     (.debug js/console "Wrapping MD base: " base)
     {:context md.context/base
      :value base}
     ($ component {& props}))))

(defn wrap-refresh [component period]
  (fnc MD [props]
    (.debug js/console "Wrapping MD refresh: " period)
    (provider
     {:context md.context/refresh-period
      :value period}
     ($ component {& props}))))

(defn wrap-show [component md-props]
  (fnc MD [props]
    (provider
     {:context md.context/show
      :value md-props}
     ($ component {& props}))))

(defn check-diff
  [a b]
  (and
   (= (:content a) (:content b))
   (= (:style a) (:style b))))

(defnc ^{:private true} show*
  {:wrap [(ui/forward-ref)
          (memo check-diff)]}
  [{:keys [class content style]} editor]
  (let [publish (toddler/use-toddler-publisher)]
    (publish {:topic :react/dangerously-rendered})
    (hooks/use-effect
      [content]
      (let [els (.querySelectorAll js/document ".copy-button")]
        (doseq [el els]
          (set! (.-innerHTML el) "Copy")
          (set! (.-onclick el)
                (fn []
                  (let [wrapper (.-parentElement el)
                        code-el (.querySelector wrapper "code")
                        code-text (.-innerText code-el)]
                    (.then (.writeText (.-clipboard js/navigator) code-text)
                           (fn []
                             (set! (.-innerHTML el) "Copied")
                             (js/setTimeout
                              #(set! (.-innerHTML el) "Copy")
                              2000)))))))))
    (d/div
     {:ref #(reset! editor %)
      :style style
      :dangerouslySetInnerHTML #js {:__html content}
      :class class})))

(defnc show
  [{:keys [content]
    p-className :className
    p-style :style
    p-class :class}]
  (let [editor (hooks/use-ref nil)
        text (hooks/use-memo
               [content]
               (when content
                 (.render md content)))
        {:keys [hash]} (router/use-location)
        scroll (hooks/use-ref nil)
        theme (toddler/use-theme)
        {:keys [on-theme-change class className]} (hooks/use-context md.context/show)
        class (or p-class class)
        className (or p-className className)]
    (hooks/use-effect
      [theme]
      (when (ifn? on-theme-change)
        (on-theme-change theme)))
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
    ($ show*
       {:content text
        :ref editor
        :style p-style
        :class (cond-> ["toddler-markdown"]
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
  {:wrap [(memo #(and (= (:url %1) (:url %2))
                      (= (:style %1) (:style %2))))]}
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
              ([_] #_(.debug js/console (str "Removing watch for URL: " url)))
                           ;;
              (async/timeout interval)
              ([_] (when (pos? interval) (recur)))))
          (fn []
            (async/close! close)))))
    ($ show {:content content & (dissoc props :url)})))

(defnc img
  {:wrap [(memo #(= %1 %2))]}
  [{:keys [src] :as props} _]
  (let [base (hooks/use-context md.context/base)
        src (str base src)]
    ($ "img"
       {:src src
        & (dissoc props :src)})))
