(ns build
  (:require
    [shadow.css.build :as cb]
    [clojure.java.io :as io]))

(defn css-release [& args]
  (let [build-state
        (-> (cb/start)
            (cb/index-path (io/file "src") {})
            (cb/index-path (io/file "showcase") {})
            (cb/generate
              '{:ui {:include [toddler.ui*]}
                :dev {:include [toddler.dev]}})
            (cb/write-outputs-to (io/file "release" "css")))]

    (doseq [mod (:outputs build-state)
            {:keys [warning-type] :as warning} (:warnings mod)]

      (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))))


(defn generate-shadow-index-file
  []
  (-> (cb/start)
      (cb/index-path (io/file "src") {})
      (cb/write-index-to (io/file "src" "shadow-css-index.edn"))))


(comment
  (let [build-state
        (-> (cb/start)
            (cb/index-path (io/file "src") {})
            (cb/index-path (io/file "showcase") {}))]

    (doseq [mod (:outputs build-state)
            {:keys [warning-type] :as warning} (:warnings mod)]

      (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))))
