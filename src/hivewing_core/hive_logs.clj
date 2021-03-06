(ns hivewing-core.hive-logs
  (:require
            [hivewing-core.configuration :refer [sql-db]]
            [taoensso.timbre :as logger]
            [digest :as digest]
            [hivewing-core.core :refer [ensure-uuid]]
            [clj-time.core :as ctime]
            [clj-time.coerce :as ctimec]
            [clojure.java.jdbc :as jdbc]))

(defn hive-logs-purge-worker
  [worker-uuid]
  (jdbc/delete! sql-db :hivelogs ["worker_uuid = ?" (ensure-uuid worker-uuid)]))

(defn hive-logs-read
  "Read hive logs, with a simple filter available.
  You can read logs sequentially, filtered by hive, worker (maybe)
  and given a starting timestamp, it will return up to cnt records."
  [hive-uuid & args]
    (let [args (apply hash-map args)
          start-at (or (:start-at args) (java.sql.Timestamp. (.getTime (java.util.Date.))))
          end-at   (:end-at args)
          worker-uuid (ensure-uuid (:worker-uuid args))
          task (:task args)
          hive-uuid (ensure-uuid hive-uuid)
          query-array (filter identity [(str
                       "SELECT * FROM hivelogs WHERE at < ? "
                       " AND hive_uuid = ? "
                       (if end-at " AND at >= ?")
                       (if (contains? args :worker-uuid)
                         (if (nil? worker-uuid) " AND worker_uuid IS NULL "
                                                " AND worker_uuid = ? "))
                       (if task " AND task = ? "
                         (if (contains? args :task) " AND task IS NULL "))
                       " ORDER BY at DESC LIMIT 250")

                      start-at
                      hive-uuid
                      (if end-at end-at)
                      (if worker-uuid worker-uuid)
                      (if task task)
                      ])
          ]
        (jdbc/query sql-db query-array)))


(defn- log-message-chunk
  "Chunks a log message into separate parts"
  ([s len] (log-message-chunk s len []))
  ([s len chunks]
    (if (<= (count s) len)
      (conj chunks s)
      (recur (subs s len) len (conj chunks (subs s 0 len))))))

(defn hive-logs-push
  "Push some hive logs
  Given a hive / worker (optional) - and task name.
  If there is no task name, it is considered a 'system-log'
  "
  [hive-uuid worker-uuid task message]
  (let [args {:worker_uuid (ensure-uuid worker-uuid)
                                     :hive_uuid   (ensure-uuid hive-uuid)
                                     :task        task
                                     :message     message}]
    (let [res (first (jdbc/insert! sql-db :hivelogs args))
         expiration-time (ctimec/to-sql-time (ctime/minus (ctime/now) (ctime/weeks 1)))]
      (jdbc/delete! sql-db :hivelogs ["hive_uuid = ? AND at < ?" (ensure-uuid hive-uuid) expiration-time])
      res
      )))
