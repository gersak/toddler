(ns toddler.showcase.prosemirror
  (:require
   [helix.core :refer [defnc $]]
   [helix.dom :as d]
   [helix.hooks :as hooks]
   [toddler.i18n.keyword :refer [add-translations]]
   ["prosemirror-view" :refer [EditorView]]
   ["prosemirror-state" :refer [EditorState]]
   ["prosemirror-markdown" :refer [schema defaultMarkdownParser defaultMarkdownSerializer]]
   ["prosemirror-example-setup" :refer [exampleSetup]]))

(add-translations
 (merge
  #:showcase.prosemirror {:default "Markdown"
                          :hr "Markdown"}))

(defnc Editor
  []
  (let [_editor (hooks/use-ref nil)
        prosemirror (hooks/use-ref nil)
        content "# Hi from Markdown
This is just some new text
```clojure
Showinng code quote
```"]
    (hooks/use-effect
      :always
      (when-not @prosemirror
        (and @_editor
             (reset! prosemirror
                     (EditorView.
                      @_editor
                      #js {:state (.create EditorState
                                           #js {:doc (.parse defaultMarkdownParser content)
                                                :plugins (exampleSetup #js {:schema schema})})})))))
    (d/div
     {:ref #(reset! _editor %)
      :className "markdown-editor"})))

(defnc ProseMirror
  []
  ($ Editor))
