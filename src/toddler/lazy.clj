(ns toddler.lazy)

(defmacro load
  ([_name & bindings]
   (let [ks (take-nth 2 bindings)]
     `(do
        (toddler.lazy/load* ~_name #(hash-map ~@bindings))
        ~@(for [k ks]
            `(helix.core/defnc ~(symbol (name k))
               [props#]
               (helix.core/$ (toddler.lazy/use-component ~k) {& props#})))))))

#_(defmacro load
    ([_name & bindings]))
