(ns hivewing-core.hive
  (:require [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]))


(defn hive-get
  "Gets the data for a given hive"
  [hive-uuid]
  (jdbs/query sql-db ["SELECT * FROM hives WHERE uuid = ?" hive-uuid]))

(defn hive-create
  "Creates a new hive"
  [apiary-uuid & parameters]
  (println "TODO"))
