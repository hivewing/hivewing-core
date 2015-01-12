(ns hivewing-core.hive-data
  (:require [hivewing-core.configuration :refer [sql-db]]
            [taoensso.timbre :as logger]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]
            ))
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
      (map :name (jdbc/query sql-db ["SELECT name FROM hivedata WHERE hive_uuid = ? AND worker_uuid IS NULL GROUP BY name" (ensure-uuid hive-uuid)])))
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

    (jdbc/insert! sql-db :hivedata args)
    (jdbc/delete! sql-db :hivedata
                    (filter identity [sql-str
                     (:hive_uuid args)
                     (if (:worker_uuid args) (:worker_uuid args))]))))

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
