(ns hivewing-core.worker-data
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.pubsub :as pubsub]
            [taoensso.carmine :as car ]
            [clojure.java.jdbc :as jdbc]
            [environ.core  :refer [env]]))

(def redis-connection {:pool {} :spec {:uri (env :hivewing-redis-uri)}})
(defmacro redis [& body] `(car/wcar redis-connection ~@body))

(defn worker-data-system-name?
  "Determines if the worker data name is a system event name"
  [name]
  (boolean (re-find #"^\." name)))

(defn worker-data-valid-name?
  "Determines if the worker data has a valid name"
  [name]
  (boolean (re-find #"^((\.)?[a-zA-Z0-9_\-])+" (clojure.core/name name))))

(defn worker-data-channel
  "Generates the channel worker data update events are sent on"
  [worker-uuid]
  (str "worker:" worker-uuid ":data"))

(defn worker-data-key-name
  "Converts a worker-uuid and then data field name to a key"
  [worker-uuid field-name]
  (str "worker-data::" worker-uuid "::" (clojure.string/replace field-name #"\[\]$" "")))

(defn worker-data-store
  "Stores the worker event data"
  [ worker-uuid & args ]
  (let [parameters (apply hash-map args)
        clean-parameters (select-keys parameters (filter worker-data-valid-name? (keys parameters)))]
    (if (not (empty? clean-parameters))
      (doseq [kv-pair clean-parameters]
        (let [data-name (clojure.core/name (key kv-pair))
              data-value (val kv-pair)
              key-name (worker-data-key-name worker-uuid data-name)]

          ; Store the data (data sequence or just a value)
          (redis (car/zadd key-name (System/currentTimeMillis) data-value)
                 (car/zremrangebyrank key-name 0 -101))))

          ; Publish that it was stored
      (pubsub/publish-message (worker-data-channel worker-uuid) clean-parameters)
    )
    clean-parameters))

(defn worker-data-keys
  "Discover the keys for a given worker."
  [worker-uuid]
  (apply vector (map #(last (clojure.string/split %1 #"::")) (redis (car/keys (worker-data-key-name worker-uuid "*"))))))

(defn worker-data-read
  "Read a given worker and it's specific data key"
  [worker-uuid field-name]
  (let [data-name (clojure.core/name field-name)
        key-name (worker-data-key-name worker-uuid data-name)]
    (hash-map field-name (apply hash-map (redis (car/zrange key-name 0 -1 :WITHSCORES))))))
