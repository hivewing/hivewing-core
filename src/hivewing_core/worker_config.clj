(ns hivewing-core.worker-config
  (:require [rotary.client :refer :all]
            [hivewing-core.configuration :refer [aws-credentials]]
            [environ.core  :refer [env]]))

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
  If you give a non-existent worker-uuid you get back {}"
  [worker-uuid]
  (let [result (query aws-credentials ddb-worker-table {"uuid" worker-uuid})
        items (:items result)
        kv-pairs (map #(hash-map (get % "key") (select-keys % ["data" "_uat" "type"])) items)
        result (into {} kv-pairs)
        ]
    result
    ))

(defn worker-config-set
  "Set the configuration on the worker. Provided a uuid and the paramters as a hash.
  The keys for the parameters should be strings or keywords"
  [worker-uuid parameters]
  ; Want to split the parameters
  (doseq [kv-pair parameters]
    (let [upload-data {:uuid worker-uuid,
               "key"  (name (get kv-pair 0)),
               "data" (str (get kv-pair 1)),
               "_uat" (System/currentTimeMillis),
               }]
      (put-item aws-credentials ddb-worker-table upload-data))))
