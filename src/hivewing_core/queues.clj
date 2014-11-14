(ns hivewing-core.queues
  (:require [taoensso.carmine :as car ]
            [environ.core :refer [env]]))

; Squelch SQS logging
(.setLevel (java.util.logging.Logger/getLogger "com.amazonaws")
             java.util.logging.Level/WARNING)

(def sqs-connection (sqs/create-client ))
; :ServiceURL (env :hivewing-redis-uri)]))

(defn create-queue
  "Create a queue for using with the enqueue-message"
  [name]
  (sqs/create-queue sqs-connection name))

(defn enqueue-message
  "Publishing a message to SQS on a given channel"
  [channel message]
    (println "TODO")
  )
