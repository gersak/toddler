(ns shadow-css-indexes
  (:require
   [clojure.java.io :as io]
   [shadow.css.build :as cb]
   [shadow.cljs.devtools.server.fs-watch :as fs-watch]
   [toddler.ui.css :as css])
  (:import java.util.zip.ZipInputStream))

(defonce css-ref (atom nil))
(defonce css-watch-ref (atom nil))

(defn init []
  (->
   (cb/init)
   (update :aliases merge css/aliases)
   (cb/start)
   (cb/index-path (io/file "src") {})))

(defn generate-indexes
  ([]
   (->
    (cb/init)
    (update :aliases merge css/aliases)
    (cb/start)
    (cb/index-path (io/file "src") {})
    (cb/generate '{:ui {:include [toddler.ui
                                  toddler.ui*
                                  toddler.docs]}})
    (cb/write-index-to (io/file "resources" "shadow-css-index.edn")))))

(defn release
  [& _]
  (init)
  (generate-indexes))
