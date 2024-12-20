(ns toddler.graphql.keys
  (:require
    [camel-snake-kebab.core :as csk]))


(defn clj [k] 
  (csk/->kebab-case-keyword k :separator #"[\s-_\&]+"))


(defn gql [k]
  (csk/->snake_case_keyword k :separator #"[\s-\&]+"))
