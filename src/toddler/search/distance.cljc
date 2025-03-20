(ns toddler.search.distance)

(defn levenshtein
  "Compute the Levenshtein distance between two strings."
  [s1 s2]
  (let [len1 (count s1)
        len2 (count s2)
        matrix (vec (map vec (repeat (inc len1) (range (inc len2)))))]

    (loop [i 1 matrix matrix]
      (if (> i len1)
        (get-in matrix [len1 len2])
        (recur (inc i)
               (loop [j 1 m matrix]
                 (if (> j len2)
                   m
                   (recur (inc j)
                          (assoc-in m [i j]
                                    (min (inc (get-in m [(dec i) j]))   ;; Deletion
                                         (inc (get-in m [i (dec j)]))   ;; Insertion
                                         (+ (get-in m [(dec i) (dec j)])
                                            (if (= (nth s1 (dec i)) (nth s2 (dec j))) 0 1))))))))))))

(comment
  (time (levenshtein "ckisie" "closure"))
  (time (levenshtein "kitten" "sitting"))
  (levenshtein "search" "searc"))
