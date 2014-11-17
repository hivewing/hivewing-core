(ns hivewing-core.hive
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))


(defn hive-get-access
  "Gets access roles for a given uuid and token"
  [hive-uuid token]
  (jdbc/query sql-db ["SELECT can_create, can_read, beekeeper_uuid FROM hive_managers WHERE hive_uuid = ? AND hive_managers.uuid = ? LIMIT 1" (ensure-uuid hive-uuid) (ensure-uuid token)] :result-set-fn first))

(defn hive-get
  "Gets the data for a given hive"
  [hive-uuid]
  (jdbc/query sql-db ["SELECT * FROM hives WHERE uuid = ? LIMIT 1" (ensure-uuid hive-uuid)] :result-set-fn first))
(defn hive-create
  "Creates a new hive"
  [{apiary-uuid :apiary_uuid :as parameters}]
  (let [clean-params (assoc parameters
                            :apiary_uuid (ensure-uuid apiary-uuid))]
    (first (jdbc/insert! sql-db :hives parameters))))
