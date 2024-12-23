(ns toddler.layout
  (:require
   [helix.core
    :refer [defnc create-context
            defhook provider
            fnc $]]
   [helix.hooks :as hooks]
   [helix.dom :as d]
   [helix.children :as c]
   [vura.core :as vura]
    ; [toddler.ui :refer [forward-ref]]
   [toddler.app :as app]
   [toddler.hooks
    :refer [use-dimensions]]))

(defhook use-layout
  ([] (hooks/use-context app/layout))
  ([k] (get (hooks/use-context app/layout) k)))

(defhook use-m-columns
  "Hook will return data distributed to columns-count number
  of vectors, distributed by pattern that goes sequentialy through
  original data sequence and appending to column = mod(idx, coulumn-count)"
  [data columns-count]
  (hooks/use-memo
    [data columns-count]
    (loop [[c & r :as data] (seq data)
           idx 0
           result (vec (take columns-count (repeat [])))]
      (if (empty? data)
        result
        (recur r (mod (inc idx) columns-count) (update result idx conj c))))))

(defhook use-columns-frame
  ([{:keys [column-width container-width max-columns padding-x]
     :or {column-width 400
          max-columns 4
          padding-x 32}
     :as props}]
   (let [estimated-size (vura/round-number container-width (+ column-width padding-x) :floor)
         column-count (hooks/use-memo
                        [column-width container-width]
                        (min (quot estimated-size column-width)
                             max-columns))]
     (assoc props
       :estimated-width estimated-size
       :column-count column-count))))

(def ^:dynamic ^js *container* (create-context nil))
(def ^:dynamic ^js *container-dimensions* (create-context nil))
(def ^:dynamic ^js *container-style* (create-context nil))

(defhook use-container [] (hooks/use-context *container*))
(defhook use-container-dimensions [] (hooks/use-context *container-dimensions*))

(letfn [(same? [a b]
          (let [ks [:style :className]
                before (select-keys a ks)
                after (select-keys b ks)
                result (= before after)]
            result))]
  (defnc Container
    [props]
    ;; TODO - maybe remove this
    ; {:wrap [(memo same?)]}
    (let [[container dimensions] (use-dimensions)]
      (d/div
       {:ref #(reset! container %)
        & props}
       (provider
        {:context *container-dimensions*
         :value dimensions}
        (provider
         {:context *container*
          :value container}
         (c/children props)))))))

(defn wrap-container
  ([component]
   (fnc Container [props]
        ($ Container ($ component {& props}))))
  ([component cprops]
   (fnc [props]
        ($ Container {& cprops} ($ component {& props})))))

; (defnc Column
;   [{:keys [label className position] :as props} _ref]
;   {:wrap [(forward-ref)]}
;   (d/div
;     {:ref _ref
;      :className className
;      :position position}
;     (when label
;       (d/div
;         {:className "label"}
;         (d/label label)))
;     (c/children props)))

; (defnc Row
;   [{:keys [label className position] :as props} _ref]
;   {:wrap [(forward-ref)]}
;   (d/div
;     {:ref _ref
;      :className className
;      :position position}
;     (when label
;       (d/div
;         {:className "label"}
;         (d/label label)))
;     (c/children props)))
