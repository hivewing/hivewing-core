(ns hivewing-core.worker-config
  (:require
            [amazonica.aws.dynamodbv2 :as ddb]
            [hivewing-core.configuration :refer [ddb-aws-credentials]]
            [hivewing-core.worker :as worker]
            [hivewing-core.pubsub :as pubsub]
            [environ.core  :refer [env]]))
(comment
    (worker-config-set "123" {".hive-images" "http://123456"})
  )
;ddb-aws-credentials

(defn worker-config-updates-channel
  "Generates the channel worker config updates are on"
  [worker-uuid]
  (str "worker-config:" worker-uuid ":updates"))

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

(defn worker-ensure-tables [ & opt]
  "Setup the worker table just like we need it set up.  uuid string, and key range-key"
  (if (= opt :delete-first)
    (ddb/delete-table ddb-aws-credentials :table-name ddb-worker-table))

  (try
    (ddb/describe-table ddb-aws-credentials :table-name ddb-worker-table)
    (catch com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException e
      (ddb/create-table ddb-aws-credentials
                        :table-name ddb-worker-table
                        :key-schema [{:attribute-name "uuid" :key-type "HASH"}
                                     {:attribute-name "key"  :key-type "RANGE"}]
                        :attribute-definitions
                                    [{:attribute-name "uuid" :attribute-type "S"}
                                     {:attribute-name "key"  :attribute-type "S"}]
                                     ;{:attribute-name "_uat" :attribute-type "N"}
                                     ;{:attribute-name "data" :attribute-type "S"}]
                        :provisioned-throughput
                                    {:read-capacity-units 1
                                     :write-capacity-units 1}))))


(defn worker-config-get
  "Given a valid worker-uuid, it will return the configuration 'hash'.
  If you give a non-existent worker-uuid you get back {}
  Pass in :include-system-keys to get back *all* keys (including system ones)"
  [worker-uuid & params-array]
  (let [params (apply hash-map params-array)
        include-system-keys? (:include-system-keys params)
        result (ddb/query ddb-aws-credentials
                          :table-name ddb-worker-table
                          :limit 1
                          :select "ALL_ATTRIBUTES"
                          :key-conditions
                            {:uuid {:attribute-value-list [(str worker-uuid)] :comparison-operator "EQ"}}
                            )
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
                 }
            old-data (ddb/put-item ddb-aws-credentials
                      :table-name ddb-worker-table
                      :item upload-data
                      :return-values "ALL_OLD")]
        (println old-data)

        (if (not (= (get upload-data "data" ) (get-in old-data [:attributes :data])))
          (if (not suppress-change-publication)
            (pubsub/publish-message (worker-config-updates-channel worker-uuid) clean-parameters)))))))

(defn worker-config-set-hive-image
  [worker-uuid hive-image-url]
    (worker-config-set worker-uuid {".hive-image" hive-image-url}))

(defn worker-config-update-hive-image-url
  "Set this value on every worker in the given hive.
  Will publish a change message for any worker which
  did not have that config set already"
  ([hive-uuid hive-image-url]
    (worker-config-update-hive-image-url hive-uuid hive-image-url 1 100))

  ([hive-uuid hive-image-url page per-page]
    (let [worker-uuids (worker/worker-list hive-uuid :per-page per-page :page page)]
      (if (not (empty? worker-uuids))
        (pmap
          #(worker-config-set-hive-image %1 hive-image-url)
          worker-uuids
          )
        (recur hive-uuid hive-image-url (+ 1 page) per-page)
        ))))
