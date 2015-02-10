(ns hivewing-core.hive-manager
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.public-key-notification :as pkn]
            [clojure.java.jdbc :as jdbc]))

(defn hive-manager-create
  "Create a hive manager record, with the given roles."
  [hive-uuid beekeeper-uuid & roles ]
  (let [clean-hive (ensure-uuid hive-uuid)
        clean-bk   (ensure-uuid beekeeper-uuid)
        roles-hsh  (apply hash-map roles)
        parameters (into {:hive_uuid clean-hive :beekeeper_uuid clean-bk } roles-hsh)
        res (first (jdbc/insert! sql-db :hive_managers parameters))
        ]
      (pkn/public-keys-notify-of-hive-change hive-uuid)
      res
    ))

(defn hive-managers-managing
  [bk-uuid]
  (jdbc/query sql-db ["SELECT * FROM hive_managers WHERE beekeeper_uuid = ?" (ensure-uuid bk-uuid)]))

(defn hive-managers-get-public-keys
  "Get all the public-keys for all the managers of a hive"
  [hive-uuid]
  (jdbc/query sql-db [(str "SELECT key FROM public_keys "
                      " INNER JOIN hive_managers ON public_keys.beekeeper_uuid = hive_managers.beekeeper_uuid "
                      " WHERE hive_managers.hive_uuid = ?") (ensure-uuid hive-uuid)]))

(defn hive-managers-get
  "Get all the hive managers for a hive"
  [hive-uuid]
  (jdbc/query sql-db ["SELECT * FROM hive_managers WHERE hive_uuid = ?" (ensure-uuid hive-uuid)]))

(defn hive-manager-delete
  "Delete a hive manager"
  [hive-manager-uuid]
  (let [hmu (ensure-uuid hive-manager-uuid)
        hive-uuid (:hive_uuid (first (jdbc/query sql-db ["SELECT hive_uuid FROM hive_managers WHERE uuid = ?" (ensure-uuid hive-manager-uuid)])))
        res (jdbc/delete! sql-db :hive_managers ["uuid = ?" hmu])
        ]
      (pkn/public-keys-notify-of-hive-change hive-uuid)
      res))
