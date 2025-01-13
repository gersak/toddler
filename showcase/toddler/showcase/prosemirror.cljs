(ns toddler.showcase.prosemirror
  (:require
   [helix.core :refer [defnc $]]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.core :as toddler]
   [toddler.layout :as layout]
   [toddler.i18n.keyword :refer [add-translations]]
   [toddler.md :as md]))

(add-translations
 (merge
  #:showcase.prosemirror {:default "Markdown"
                          :hr "Markdown"}))

; (defnc md
;   [{:keys [content class className] :as props}]
;   (let [_editor (hooks/use-ref nil)
;         prosemirror (hooks/use-ref nil)
;         schema (hooks/use-memo
;                  :once
;                  (let [nodes (.. schema -spec -nodes)
;                        _ (.log js/console nodes)
;                        code-block (.get nodes "code_block")
;                        _ (when code-block
;                            (set! (.-toDOM code-block)
;                                  (fn toNode [node]
;                                    (let [language (.. node -attrs -params)
;                                          code-content (.-textContent node)
;                                          el (js/document.createElement "code")]
;                                      (.setAttribute el "class" (str "language-" language))
;                                      (set! (.-innerHTML el) (.-value (.highlight hljs language code-content)))
;                                      #js ["pre" #js {:class "code"} el]))))
;                        nodes (.append nodes
;                                       #js {:code_block code-block})]
;                    (Schema.
;                     #js {:nodes nodes
;                          :marks (.. schema -spec -marks)})))]
;     (hooks/use-effect
;       :always
;       (when-not @prosemirror
;         (and @_editor
;              (not-empty content)
;              (reset! prosemirror
;                      (EditorView.
;                       @_editor
;                       #js {:state (.create EditorState
;                                            #js {:doc (.parse defaultMarkdownParser content)
;                                                 :schema schema})
;                            :editable (constantly false)})))
;         (refresh-highlight)))
;     (d/div
;      {:ref #(reset! _editor %)
;       :class (cond-> [$info "markdown-editor"]
;                (string? className) (conj className)
;                (string? class) (conj class)
;                (sequential? class) (into class))
;       & (dissoc props :class :className :content)})))

; (defnc url-md
;   [{:keys [url] :as props}]
;   (let [[content set-content!] (hooks/use-state nil)]
;     (hooks/use-effect
;       :always
;       (-> (js/fetch url)
;           (.then
;            (fn [response]
;              (if (.-ok response)
;                (-> (.text response)
;                    (.then (fn [text] (set-content! text)))
;                    (.catch (fn [err] (.err js/console "Couldn't extract text" err))))
;                (throw (js/Error (str "Failed to fetch: " url))))))
;           (.catch
;            (fn [err]
;              (.err js/console (str "Failed fetching file: " url))))))
;     ($ md {:content content & (dissoc props :url)})))

(defnc ProseMirror
  []
  (let [{:keys [height width]} (layout/use-container-dimensions)
        translate (toddler/use-translate)]
    ($ ui/simplebar
       {:style {:height height
                :width width}}
       ($ ui/row {:align :center}
          ($ ui/column
             {:align :center
              :style {:max-width "40rem"}}
             ($ md/watch-url
                {:url "/doc/en/i18n.md"}))))))
