(ns hivewing-core.hive-data
  (:require [hivewing-core.configuration :as config :refer [sql-db]]
            [taoensso.timbre :as logger]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]
            [amazonica.aws.sqs :as sqs]
            [environ.core  :refer [env]]))

(defn hive-data-sqs-queue
  "In dev mode we piggy back on SQS instead of kinesis"
  []
  (let [queue-name (or (env :hivewing-sqs-hive-data-queue ) "hive-kinesis-queue")
         queue (sqs/find-queue config/sqs-aws-credentials queue-name)]
        (if queue
          queue
          (:queue-url (sqs/create-queue
              config/sqs-aws-credentials
              :queue-name queue-name
              :attributes
                {:VisibilityTimeout 30 ; sec
                 :MaximumMessageSize 2048 ; bytes
                 :MessageRetentionPeriod 3600 ; sec
                 :ReceiveMessageWaitTimeSeconds 0})) ; sec
          )))

(defn hive-data-push-restart-hive-processing
  "Push a message which indicates to restart hive processing"
  [hive-uuid]
  (try
    (sqs/send-message config/sqs-aws-credentials
                    (hive-data-sqs-queue)
                    (prn-str {:hive-uuid (str hive-uuid)
                              :restart   true}))
  (catch Exception e (println e))))

(defn hive-data-push-to-processing
  [hive-uuid worker-uuid data-name data-value at]
  (logger/info "Push to processing:" hive-uuid worker-uuid data-name data-value at)
  (try
    (sqs/send-message config/sqs-aws-credentials
                    (hive-data-sqs-queue)
                    (prn-str {:hive-uuid (str hive-uuid)
                              :worker-uuid (str worker-uuid)
                              :data-name (str data-name)
                              :data-value data-value
                              :at (.getTime at)}))
  (catch Exception e (println e)))
  )

(def hive-data-keep-count
  "The number of data records to keep for each data value"
  25)

(defn hive-data-purge-worker
  "Delete all the data for a given worker"
  [worker-uuid]
  (jdbc/delete! sql-db :hivedata ["worker_uuid = ?" (ensure-uuid worker-uuid)]))

(defn hive-data-get-keys
  "Retrieve a listing of all the currently valid hive-data keys in the system.
  Can scope to just a single worker or across all the workers in a hive"
  ([hive-uuid] (hive-data-get-keys hive-uuid nil))
  ([hive-uuid worker-uuid]
    (if (nil? worker-uuid)
      (map :name (jdbc/query sql-db ["SELECT name FROM hivedata WHERE hive_uuid = ? AND worker_uuid IS NULL GROUP BY name" (ensure-uuid hive-uuid)]))
      (map :name (jdbc/query sql-db ["SELECT name FROM hivedata WHERE hive_uuid = ? AND worker_uuid = ? GROUP BY name" (ensure-uuid hive-uuid) (ensure-uuid worker-uuid)])))))


(comment
  (jdbc/query sql-db ["SELECT * FROM hivedata"])
  (jdbc/query sql-db ["SELECT name FROM hivedata GROUP BY name"])
  (def hive-uuid "d759d664-9a77-11e4-9505-0242ac11002d")
  (hive-data-get-keys hive-uuid)
)

(defn hive-data-push
  "Push some hive data!
  Given a hive / worker (optional) - and data name.
  http://stackoverflow.com/questions/5170546/how-do-i-delete-a-fixed-number-of-rows-with-sorting-in-postgresql
  "
  [hive-uuid worker-uuid data-name data-value]
  (let [args {:worker_uuid (ensure-uuid worker-uuid)
              :hive_uuid   (ensure-uuid hive-uuid)
              :name        data-name
              :data        data-value}
        inner-sql (str "SELECT ctid FROM hivedata
                                WHERE hive_uuid = ? "
                                (if (:worker_uuid args)
                                  " AND worker_uuid = ? "
                                  " AND worker_uuid IS NULL ")
                                " ORDER BY at DESC "
                                " OFFSET " hive-data-keep-count)
        sql-str (str "ctid IN ( " inner-sql ")")
        ]

    (let [res (first (jdbc/insert! sql-db :hivedata args))]
      (hive-data-push-to-processing (:hive_uuid args)
                                    (:worker_uuid args)
                                    data-name
                                    data-value
                                    (:at res))
      (jdbc/delete! sql-db :hivedata
                      (filter identity [sql-str
                       (:hive_uuid args)
                       (if (:worker_uuid args) (:worker_uuid args))])))))

(defn hive-data-read
  "Read hive data.
  You can read hive data only by individual keys
  It returns the last hive-data-keep-count records"
  [hive-uuid worker-uuid data-name & args]
    (let [args (apply hash-map args)
          worker-uuid (ensure-uuid worker-uuid)
          hive-uuid (ensure-uuid hive-uuid)
          limit (or (:limit args) hive-data-keep-count)
          query-array (filter identity [(str
                       "SELECT * FROM hivedata WHERE "
                       " hive_uuid = ? "
                       (if (nil? worker-uuid) " AND worker_uuid IS NULL "
                                              " AND worker_uuid = ? ")
                       " AND name = ? "
                       " ORDER BY at DESC LIMIT ?")

                      hive-uuid
                      (if worker-uuid worker-uuid)
                      data-name
                      limit
                      ])
          ]
        (jdbc/query sql-db query-array)))
