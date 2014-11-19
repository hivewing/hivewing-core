(ns hivewing-core.worker-config
  (:require [rotary.client :refer :all]
            [hivewing-core.configuration :refer [aws-credentials]]
            [hivewing-core.pubsub :as pubsub]
            [environ.core  :refer [env]]))

(defn worker-config-system-name?
  "Determines if the worker config system name is a system name"
  [name]
  (boolean (re-find #"^\." name)))

(defn worker-config-valid-name?
  "Determines if the worker config is a valid name"
  [name]
  (boolean (re-find #"^((\.)?[a-zA-Z0-9_\-])+" name)))

(def ddb-worker-table
  "The DDB table which stores the configuration"
  (env :hivewing-ddb-worker-config-table))

(defn worker-ensure-tables []
  "Setup the worker table just like we need it set up.  uuid string, and key range-key"
  (ensure-table aws-credentials {:name ddb-worker-table,
                                 :hash-key {:name "uuid", :type :s},
                                 :range-key {:name "key", :type :s},
                                 :throughput {:read 1, :write 1}}))

(defn worker-config-get
  "Given a valid worker-uuid, it will return the configuration 'hash'.
  If you give a non-existent worker-uuid you get back {}
  Pass in :include-system-keys to get back *all* keys (including system ones)"
  [worker-uuid & params-array]
  (let [params (apply hash-map params-array)
        include-system-keys? (:include-system-keys params)
        result (query aws-credentials ddb-worker-table {"uuid" (str worker-uuid)})
        items (:items result)
        ; Filter out the system keys if needed
        filtered-items (if include-system-keys?
                         items
                         (remove #(worker-config-system-name? (get %1 "key")) items))
        ; Now map those to a hash key structure
        kv-pairs (map #(hash-map (get % "key") (get % "data")) filtered-items)
        ; Now we have a result!
        result (into {} kv-pairs)]
    result
    ))

(defn worker-config-watch-changes
  "Watch for changes to this worker config.
  Get the hash that was updated if it does get updated"
  [worker-uuid handler]
    (pubsub/subscribe-change "worker-config" worker-uuid handler))
(defn worker-config-stop-watching-changes
  [listener]
  (pubsub/unsubscribe listener))

(defn worker-config-set
  "Set the configuration on the worker. Provided a uuid and the paramters as a hash.
  The keys for the parameters should be strings or keywords.
  Returns true if it worked"
  [worker-uuid parameters & args]
  ; Want to split the parameters
  (let [clean-parameters (select-keys parameters (filter #(worker-config-valid-name? %1) (keys parameters)))
        suppress-change-publication (:suppress-change-publication (apply hash-map args)) ]
    (doseq [kv-pair clean-parameters]
      (let [upload-data {"uuid" (str worker-uuid)
                         "key"  (name (get kv-pair 0)),
                         "_uat" (System/currentTimeMillis),
                         "data" (str (get kv-pair 1))
                 }]
        (put-item aws-credentials ddb-worker-table upload-data)))
      (if (not suppress-change-publication)
        (pubsub/publish-change "worker-config" worker-uuid clean-parameters))))
