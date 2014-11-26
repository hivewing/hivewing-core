(ns hivewing-core.hive-manager
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn hive-manager-create
  "Create a hive manager record, with the given roles."
  [hive-uuid beekeeper-uuid & roles ]
  (let [clean-hive (ensure-uuid hive-uuid)
        clean-bk   (ensure-uuid beekeeper-uuid)
        roles-hsh  (apply hash-map roles)
        parameters (into {:hive_uuid clean-hive :beekeeper_uuid clean-bk } roles-hsh) ]
    (first (jdbc/insert! sql-db :hive_managers parameters))))

(defn hive-managers-get
  "Get all the hive managers for a hive"
  [hive-uuid]
  (jdbc/query sql-db ["SELECT * FROM hive_managers WHERE hive_uuid = ?" (ensure-uuid hive-uuid)]))

(defn hive-manager-delete
  "Delete a hive manager"
  [hive-uuid hive-manager-uuid]
  (let [hu (ensure-uuid hive-uuid)
        hmu (ensure-uuid hive-manager-uuid)]
    (jdbc/delete! :hive_managers ["hive_uuid = ? AND uuid = ?" hu hmu])))
