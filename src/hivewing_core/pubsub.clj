(ns hivewing-core.pubsub
  (:require [taoensso.carmine :as car ]
            [environ.core :refer [env]]))

(def redis-connection {:pool {} :spec {:uri (env :hivewing-redis-uri)}})
(defmacro redis [& body] `(car/wcar redis-connection ~@body))

(defn publish-message
  "Publish a message on pubsub , the channel is a string
   defining the pub channel. Data is whatever data you wanted."
  [channel data]
    (redis (car/publish channel data)))

(def redis-listener {:uri (env :hivewing-redis-uri)})

(defn subscribe-message
  "Subscribe to all messages from the given channel"
  [& channels-and-handlers]
  (let [channels (apply hash-map channels-and-handlers)]
    (car/with-new-pubsub-listener redis-listener
      (into {} (map
                #(vector (key %1)
                         (fn [[msg_type chan data]] (if (= msg_type "message") ((val %1) data))))
                  channels))
      (doseq [chan (keys channels)]
        (car/subscribe chan)
        ))))

(defn unsubscribe
  "Unsubscribe to handler"
  [listener]
  (car/close-listener listener))
