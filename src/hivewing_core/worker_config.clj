(ns hivewing-core.worker-config
  (:require [rotary.client :refer :all]
            [environ.core  :refer [env]]))

(def ^:private aws-credentials
  "The AWS Credentials to connect to ddb"
  {:access-key (env :hivewing-aws-access-key),
   :secret-key (env :hivewing-aws-secret-key),
   :endpoint   (env :hivewing-ddb-endpoint)})

(def ddb-worker-table
  "The DDB table which stores the configuration"
  (env :hivewing-ddb-worker-config-table))

(defn worker-ensure-tables []
  "Setup the worker table just like we need it set up.  guid string, and key range-key"
  (ensure-table aws-credentials {:name ddb-worker-table,
                                 :hash-key {:name "guid", :type :s},
                                 :range-key {:name "key", :type :s},
                                 :throughput {:read 1, :write 1}}))

(defn config-get
  "Given a valid worker-guid, it will return the configuration 'hash'.
  If you give a non-existent worker-guid you get back {}"
  [worker-guid]
  (let [result (query aws-credentials ddb-worker-table {"guid" worker-guid})
        items (:items result)
        kv-pairs (map #(hash-map (get % "key") (select-keys % ["data" "_uat" "type"])) items)
        result   (reduce #(merge %1 %2) kv-pairs)
        ]
    (println items)
    (println kv-pairs)
    result
    ))

(defn config-set
  "Set the configuration on the worker. Provided a guid and the paramters as a hash.
  The keys for the parameters should be strings or keywords"
  [worker-guid parameters]
  ; Want to split the parameters
  (doseq [kv-pair parameters]
    (let [upload-data {:guid worker-guid,
               "key"  (name (get kv-pair 0)),
               "data" (str (get kv-pair 1)),
               "_uat" (System/currentTimeMillis),
               }]
      (put-item aws-credentials ddb-worker-table upload-data))))
