(ns hivewing-core.hive-image-notification
  (:require [clojure.java.shell :as shell]
            [taoensso.timbre :as logger]
            [clojure.java.io :as io]
            [biscuit.core :as crc]
            [amazonica.aws.sqs :as sqs]
            [amazonica.aws.s3 :as s3]
            [amazonica.aws.s3transfer :as s3-transfer]
            [digest :as digest]
            [hivewing-core.configuration :as config]
            [environ.core  :refer [env]]))

(defn hive-images-notification-sqs-queue
  "The channel which all the changes to images should come in.
  They are JSON blobs which have a type and a payload.
  Types are:
     'hive-update'  -> a hive was updated (created / deleted).
                       update it's repo and access, and make sure
                       the image_ref in the hive is updated.
     'beekeeper-update'  -> a beekeeper was update (created / deleted).
                       add it to the gitolite, make sure other hives don't include him
     'image-update' -> an image was updated
                       make sure the image_ref in the hive is updated.
     'worker-update' -> a worker was updated and needs to make sure
                        it has the right image.
  "
  []
  (let [queue-name (env :hivewing-sqs-hive-images-queue )
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
                 :ReceiveMessageWaitTimeSeconds 10})) ; sec
          )))

;
(defn hive-images-notification-send-hive-update-message
  "Send a message that a hive was updated (created / deleted)"
  [hive-uuid]
  (logger/info "Updated hive: " hive-uuid)
  (sqs/send-message config/sqs-aws-credentials (hive-images-notification-sqs-queue) (prn-str {:hive-update hive-uuid})))

(defn hive-images-notification-send-beekeeper-update-message
  "Send a message that a beekeeper was updated / created / deleted"
  [beekeeper-uuid]
  (logger/info "Updated beekeeper: " beekeeper-uuid)
  (sqs/send-message config/sqs-aws-credentials (hive-images-notification-sqs-queue) (prn-str {:beekeeper-update beekeeper-uuid})))

(defn hive-images-notification-send-images-update-message
  "Send a message that a image was updated / created / deleted"
  [hive-uuid]
  (logger/info "Updated hive image " hive-uuid)
  (sqs/send-message config/sqs-aws-credentials (hive-images-notification-sqs-queue) (prn-str {:image-update hive-uuid})))

(defn hive-images-notification-send-worker-update-message
  "Send a message that a worker was moved between hives"
  [worker-uuid]
  (logger/info "Updated worker" worker-uuid)
  (sqs/send-message config/sqs-aws-credentials (hive-images-notification-sqs-queue) (prn-str {:worker-update worker-uuid})))
