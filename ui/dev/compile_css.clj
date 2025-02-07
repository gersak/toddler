(ns compile-css
  (:require
   [clojure.java.io :as io]
   [shadow.css.build :as cb]
   [shadow.cljs.devtools.server.fs-watch :as fs-watch]
   [toddler.css :as css])
  (:import java.util.zip.ZipInputStream))

(defonce css-ref (atom nil))
(defonce css-watch-ref (atom nil))

(defn init []
  (->
   (cb/init)
   (update :aliases merge css/aliases)
   (cb/start)
   (cb/index-path (io/file "../src") {})
   (cb/index-path (io/file "src" {}))))

(defn generate-indexes
  ([]
   (let [result
         (-> @css-ref
             (cb/generate '{:ui {:include [toddler.ui*
                                           toddler.md
                                           toddler.notifications
                                           toddler]}})
             (cb/write-index-to (io/file "src" "shadow-css-index.edn")))]
     (prn "Indexes generated")
     (doseq [mod (:outputs result)
             {:keys [warning-type] :as warning} (:warnings mod)]
       (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))
     (println))))

(defn release
  [& _]
  (generate-indexes))
