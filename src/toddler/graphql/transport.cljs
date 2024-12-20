(ns toddler.graphql.transport
  (:require
    [cljs-bean.core :refer [->js ->clj]]
    [clojure.core.async :as async]
    [cognitect.transit :as transit]))



(defn send-query
     ([query & 
       {:keys [on-error
               on-progress
               on-load
               operation-name 
               token
               variables
               url] :as data}]
      (let [result (async/chan)
            body #js {"operationName" operation-name 
                      "query" query
                      "variables" (clj->js variables)} 
            xhr (new js/XMLHttpRequest)]
        (.open xhr "POST" url)
        (.setRequestHeader xhr "Content-Type" "application/json")
        (.setRequestHeader xhr "Accept" "application/json")
        ;; This should be flaged by some env *variable*
        (when token
          (.setRequestHeader xhr "Authorization" (str "Bearer " token)))
        (.addEventListener xhr "error" 
                           (fn [evt] 
                             (.error js/console "Couldn't contact EYWA")
                             (let [body (.. evt -currentTarget -responseText)] 
                               (async/put!
                                 result
                                 (case (.. evt -currentTarget -status)
                                   403 (ex-info
                                         "Not authorized"
                                         {:type :not-authorized})
                                   500 (ex-info
                                         "Server couldn't process GraphQL query"
                                         {:query query
                                          :data data
                                          :body (try
                                                  (.parse js/JSON body)
                                                  (catch js/Error _ body))
                                          :type :server-error})
                                   (let [raw-data (.parse js/JSON body)
                                         data (->clj raw-data)]
                                     data)))
                               (when (ifn? on-error) (on-error evt)))))

        (when (ifn? on-progress) (.addEventListener xhr "progress" on-progress))
        (.addEventListener xhr "load" 
                           (fn [evt] 
                             (let [body (.. evt -currentTarget -responseText)] 
                               (async/put!
                                 result
                                 (case (.. evt -currentTarget -status)
                                   403 (ex-info
                                         "Not authorized"
                                         {:type :not-authorized})
                                   500 (let [{[{:keys [message]}] :errors :as response}
                                             (try
                                               (->clj (.parse js/JSON body))
                                               (catch js/Error _ body))]
                                         (ex-info
                                           (or message "Server couldn't process GraphQL query")
                                           {:query query
                                            :data data
                                            :response response 
                                            :type :server-error}))
                                   (let [raw-data (.parse js/JSON body)
                                         data (->clj raw-data)]
                                     data))))
                             (when (ifn? on-load) (on-load evt))))
        (.send xhr 
               (.stringify js/JSON body
                           (fn [_ v]
                             (cond-> v
                               (transit/uuid? v) str
                               (uuid? v) str))))
        result)))
