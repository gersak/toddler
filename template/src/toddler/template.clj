(ns toddler.template
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn random-string
  ([] (random-string 5))
  ([length]
   (let [chars "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"]
     (apply str (repeatedly length #(rand-nth chars))))))

(defn process
  "Function will take source file and target file and
  repace all {{variable}} with provided variable
  definitions."
  ([file target] (process file target nil))
  ([file target variables]
   ; (def file "template/package.json")
   ; (def variables {:project project})
   (let [content (io/resource file)]
     (assert (some? content) "File doesn't exist!")
     (let [content (slurp content)
           _variables (distinct (re-seq #"(?<=\{\{)\w+(?=\}\})" content))
           new-content (reduce
                        (fn [result variable]
                          ; (println "Replacing {{" variable "}} in " file)
                          (str/replace result
                                       (re-pattern (str "\\{\\{" variable "\\}\\}"))
                                       (get variables (keyword variable))))
                        content
                        _variables)]
       (io/make-parents target)
       (when (.exists (io/file target))
         (throw
          (ex-info "File already exists"
                   {:template file
                    :target target})))
       (spit target new-content)))))
