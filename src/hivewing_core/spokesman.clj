(ns hivewing-core.spokesman
  (:require [hivewing-core.configuration :as config]
            [taoensso.timbre :as logger]
            [amazonica.aws.sqs :as sqs]
            [environ.core  :refer [env]]
            ))
(comment
  (spokesman-queue-post-hook "http://www.google.com/" {} {"message" "testing you! Thanks for the nice URL"})
  (def target "http://www.google.com")
  (def headers {})
  (def data {"message" "testing"})
  (prn-str {:post-hook
                              { :target target
                                :headers headers
                                :data data}}))

(defn spokesman-sqs-queue
  []
  (let [queue-name (env :hivewing-sqs-spokesman-queue )
         queue (sqs/find-queue config/sqs-aws-credentials queue-name)]
        (if queue
          queue
          (:queue-url (sqs/create-queue
              config/sqs-aws-credentials
              :queue-name queue-name
              :attributes
                {:VisibilityTimeout 30 ; sec
                 :MaximumMessageSize 65536 ; bytes
                 :MessageRetentionPeriod 1209600 ; sec
                 :ReceiveMessageWaitTimeSeconds 5})) ; sec
          )))

(defn spokesman-queue-post-hook
  "Queue up the post hook.  It has a target URL
    You can put basic-auth in the URL - url must be FULL
    including scheme"
  [target headers data]
  (logger/info "Queueing post-hook " target)
  (sqs/send-message config/sqs-aws-credentials
                    (spokesman-sqs-queue)
                    (prn-str {:post-hook
                              { :target target
                                :headers headers
                                :data data}})))

(defn spokesman-queue-email
  [to subject text html & args]
  (logger/info "Queueing email: " to subject)
  (sqs/send-message config/sqs-aws-credentials (spokesman-sqs-queue)
                    (prn-str {:email
                              { :to to
                                :from "spokesman@hivewing.io"
                                :subject subject
                                :text text
                                :html html}})))
