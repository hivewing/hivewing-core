(ns hivewing-core.worker
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn worker-get
  "Get the data for worker record.  You pass in the worker via the worker uuid.  Returns the data
  as a hashmap."
  [worker-uuid]
  (jdbc/query sql-db ["SELECT * FROM workers WHERE uuid = ? LIMIT 1" (ensure-uuid worker-uuid)] :result-set-fn first))

(defn worker-create
  [parameters]
  (first (jdbc/insert! sql-db :workers parameters)))

(defn worker-join-hive
  "Add a worker to a given hive. Removing it from a hive it was in before.
  That hive MUST be part of the same apiary.  If not, you should use
  worker-join-apiary first (which sets the hive to nil)"
  [worker-uuid hive-uuid]
  (println "TODO"))

(defn worker-join-apiary
  "Adds the worker to an apiary, setting the hive_uuid to nil"
  [worker-uuid apiary-uuid]
  (println "TODO"))

(defn worker-reset-access-token
  [worker-uuid]
  (jdbc/execute! sql-db ["UPDATE workers SET access_token = uuid_generate_v4() WHERE uuid = ?" (ensure-uuid worker-uuid)])
  (worker-get (ensure-uuid worker-uuid)))
