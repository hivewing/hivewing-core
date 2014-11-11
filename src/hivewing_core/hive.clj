(ns hivewing-core.hive
  (:require [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]))


(defn hive-get
  "Gets the data for a given hive"
  [hive-uuid]
  (jdbc/query sql-db ["SELECT * FROM hives WHERE uuid = ? LIMIT 1" hive-uuid] :result-set-fn first))

(defn hive-create
  "Creates a new hive"
  [parameters]
  (first (jdbc/insert! sql-db :hives parameters)))
