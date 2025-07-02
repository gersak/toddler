(ns toddler.search.docs
  (:require
   [clojure.string :as str]
   [toddler.search :as search]))

(defn heading? [line]
  (boolean (re-find #"^\s*(#{1,6})\s+(.*)$" line)))

(defn code? [line]
  (boolean (re-find #"^\s*```" line)))

(defn join [lines]
  (str/join "\n" lines))

(defn parse-code
  [lines]
  (let [code (vec (take-while (complement code?) (rest lines)))]
    [{:type :code :body code} (drop (+ (count code) 2) lines)]))

(defn parse-section
  [lines]
  (loop [[line & lines :as all] lines
         result [nil]]
    (cond
      (or (nil? line)
          (heading? line))
      [result all]
      ;;
      (code? line)
      (let [[code lines] (parse-code all)]
        (recur lines (conj result code nil)))
      ;;
      :else
      (let [idx (dec (count result))]
        (recur
         lines
         (update result idx
                 (fn [current]
                   (as-> current data
                     (if (contains? data :type)
                       data
                       (assoc data :type :paragraph))
                     (update data :body (fnil conj []) line)))))))))

(comment
  (boolean (re-find #"^\s*#" "#### Examples"))
  (boolean (re-find #"^\s*#" "$ Examples"))
  (heading? "#### Examples"))

(defn parse-md
  [path]
  (loop [[line & lines :as all] (str/split-lines (slurp path))
         current nil
         result []]
    (if (nil? line)
      (if current
        (conj result current)
        result)
      (if-let [next-heading (re-find #"^\s*#.*" line)]
        (if (some? current)
          (recur lines {:heading next-heading} (conj result current))
          (recur lines {:heading next-heading} result))
        (if (empty? (str/trim line))
          (recur lines current result)
          (let [[section lines] (parse-section all)]
            (recur
             lines
             (update current :body (fnil into []) section)
             result)))))))

(defn prepare-section
  [{:keys [heading body] :as data}]
  (as-> data data
    (if (some? heading)
      (assoc data :id (-> heading
                          (str/replace #"^\s*#*\s*" "")
                          str/lower-case
                          str/trim
                          (str/replace #"[\s|_|']+" "-")))
      data)
    (update data :body
            (fn [sections]
              (mapv
               #(update % :body join)
               sections)))))

(defn prepare-md
  ([path] (prepare-md (str/replace path #"\..*$" "") path))
  ([route path]
   (mapv
    (fn [section]
      (assoc
       (prepare-section section)
        :route route))
    (parse-md path))))

(defn prepare-index
  [mds]
  (vec
   (mapcat
    (fn [{:keys [route topic path]}]
      (let [sections (prepare-md route path)]
        (map
         (fn [{:keys [id heading body]}]
           (let [_ref (str (gensym "doc_"))
                 content (str/join "\n" (map :body body))]
             {:ref _ref
              :meta {:route (str route "/#" id)
                     :topic topic
                     :title heading}
              :index {:title heading
                      :content content}}))
         sections)))
    mds)))

(defn make-config
  [root]
  (letfn [(->link [route]
            (str root route))]
    [{:route (->link "/rationale")
      :topic "Rationale"
      :path "showcase/docs/rationale.md"}
     {:route (->link "/calendar")
      :topic "Calendar"
      :path "showcase/docs/calendar.md"}
     {:route (->link "/i18n")
      :topic "i18n"
      :path "showcase/docs/i18n.md"}
     {:route (->link "/icons")
      :topic "Icons"
      :path "showcase/docs/icons.md"}
     {:route (->link "/inputs")
      :topic "Inputs"
      :path "showcase/docs/inputs.md"}
     {:route (->link "/layout")
      :topic "Layout"
      :path "showcase/docs/layout.md"}
     {:route (->link "/modal")
      :topic "Modal"
      :path "showcase/docs/modal.md"}
     {:route (->link "/notifications")
      :topic "Notifications"
      :path "showcase/docs/notifications.md"}
     {:route (->link "/popup")
      :topic "Popup"
      :path "showcase/docs/popup.md"}
     {:route (->link "/rationale")
      :topic "Rationale"
      :path "showcase/docs/rationale.md"}
     {:route (->link "/routing")
      :topic "Routing"
      :path "showcase/docs/routing.md"}
     {:route (->link "/tables")
      :topic "Tables"
      :path "showcase/docs/tables.md"}
     {:route (->link "/tauri")
      :topic "Tauri"
      :path "showcase/docs/tauri.md"}
     {:route (->link "/lazy")
      :topic "Lazy"
      :path "showcase/docs/lazy.md"}]))

#_(defn build-index
    ([{:keys [mds]}]
     (search/build-index (prepare-index mds))))

(defn build-index
  [{:keys [mds output] :as data}]
  (spit (str output) (search/build-index (prepare-index mds))))

(comment
  (spit "showcase/docs/docs.index.edn" (build-index (make-config "")))
  (build-index
   {:output "showcase/docs/docs.index.edn"
    :mds (make-config "")})
  (build-index
   {:output "showcase/web/docs.index.edn"
    :mds (make-config "/toddler")}))
