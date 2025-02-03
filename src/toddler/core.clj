(ns toddler.core
  (:require
   [clojure.string]))

(defmacro mlf
  "Multiline format macro. This macro will take string and
  if it is followed by something other than string will assume
  that taken string is formated line and following are arguments.
  
  Repeats until next string end of line-or-arg input
  
  ```clojure
  (let [variable \"391092109\"]
    (mlf
     \"Hi from macro\"
     \"with formated  %s  \" variable
     \"text on number %d\" 10292))
  ```"
  [& line-or-arg]
  (loop [[current & others] line-or-arg
         result []]
    (let [[next] others]
      (cond
        ;; When there is no next and there are no others
        ;; return result
        (and (nil? current) (empty? others))
        `(clojure.string/join "\n" ~result)
        ;; If current is string and there are no others, than join that line
        (and (string? current) (empty? others))
        `(clojure.string/join "\n" ~(conj result current))
        ;; If this is string and string follows than join
        ;; line in result and recur with others
        (and (string? current) (string? next))
        (recur others (conj result current))
        ;; If current is string and next isn't string, than
        ;; this should be formated
        (and (string? current) (not (string? next)))
        (let [args (take-while #(not (string? %)) others)
              line `(~'goog.string.format ~current ~@args)]
          (recur
           (drop (count args) others)
           (conj result line)))))))

(comment
  (let [variable "391092109"]
    (mlf
     "Hi from macro"
     "with formated  %s  " variable
     "text on number %d" 10292)))

;; TODO - write macro for lazy component loading using Suspense
; (defmacro lazy-component [module component]
;   `(helix.core/defnc
;      ~'react/lazy
;      (fn []
;        (->
;         (shadow.loader/load ~module)
;         (.then (fn [_] #js {:default ~'~component}))))))
