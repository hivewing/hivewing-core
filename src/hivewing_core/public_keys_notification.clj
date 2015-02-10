(ns hivewing-core.public-keys-notification
            (:require [hivewing-core.pubsub :as pubsub]))

(defn public-keys-hive-updated-channel
  "Generates the channel where public key changes for a hive are sent"
  [hive-uuid]
  (str "public-keys:" hive-uuid ":updates"))

(defn public-keys-notify-of-hive-change
  [hive-uuid]
  (pubsub/publish-message (public-keys-hive-updated-channel hive-uuid) :ignored))
