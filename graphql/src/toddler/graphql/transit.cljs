(ns toddler.graphql.transit
  (:require
   [com.cognitect.transit.types]
   [toddler.graphql :refer [->graphql GraphQLTransformProtocol]]))

(extend-protocol GraphQLTransformProtocol
  com.cognitect.transit.types/UUID (->graphql [this] (->graphql (str this))))
