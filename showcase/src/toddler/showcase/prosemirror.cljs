(ns toddler.showcase.prosemirror
  (:require
   ["react-dom" :as rdom]
   [helix.core :refer [defnc $ Suspense]]
   [shadow.css :refer [css]]
   [toddler.ui :as ui]
   [toddler.core :as toddler]
   [toddler.layout :as layout]
   [toddler.i18n.keyword :refer [add-translations]]
   [toddler.showcase.i18n :refer [i18n-example]]
   [toddler.showcase.modal :refer []]
   [toddler.md.lazy :as md]))

(add-translations
 (merge
  #:showcase.prosemirror {:default "Markdown"
                          :hr "Markdown"}))

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
              :style {:max-width "30rem"}}
             ($ md/watch-url
                {:url "/modal.md"})
             (when-some [el (.getElementById js/document "modal-background-example")]
               (rdom/createPortal ($ i18n-example) el)))))))
