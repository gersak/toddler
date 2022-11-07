(ns repl
  (:require
    [clojure.java.io :as io]
    [toddler.ui.default.color :refer [color]]
    [shadow.css.build :as cb]
    [shadow.cljs.devtools.server.fs-watch :as fs-watch]))

(defonce css-ref (atom nil))
(defonce css-watch-ref (atom nil))

(defn generate-css []
  (let [result
        (-> @css-ref
            (cb/generate '{:ui {:include [toddler.ui*]}})
            (cb/write-outputs-to (io/file "dev" "css")))]

    (prn :CSS-GENERATED)
    (doseq [mod (:outputs result)
            {:keys [warning-type] :as warning} (:warnings mod)]
      (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))
    (println)))

(defn start
  {:shadow/requires-server true}
  []

  ;; first initialize my css
  (reset! css-ref
          (-> (cb/start)
              (cb/index-path (io/file "src") {})
              (cb/index-path (io/file "showcase") {})))

  ;; then build it once
  (generate-css)

  ;; then setup the watcher that rebuilds everything on change
  (reset! css-watch-ref
          (fs-watch/start
            {}
            [(io/file "src")
             (io/file "showcase")]
            ["cljs" "cljc" "clj"]
            (fn [updates]
              (try
                (doseq [{:keys [file event]} updates
                        :when (not= event :del)]
                  ;; re-index all added or modified files
                  (swap! css-ref cb/index-file file))

                (generate-css)
                (catch Exception e
                  (prn :css-build-failure)
                  (prn e))))))

  ::started)

(defn stop []
    (when-some [css-watch @css-watch-ref]
          (fs-watch/stop css-watch)
              (reset! css-ref nil))
    
      ::stopped)

(defn go []
  (stop)
  (start))


(comment
  (-> css-ref deref keys )
  (spit "aliases.edn" (-> css-ref deref :aliases keys ))
  (go))
