(ns hivewing-core.apiary
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn apiary-find-by-beekeeper
  "Finds the apiary of a given beekeeper"
  [bk-uuid]
  (jdbc/query sql-db ["SELECT * FROM apiaries WHERE beekeeper_uuid = ? LIMIT 1" (ensure-uuid bk-uuid)] :result-set-fn first))

(defn apiary-get
  "Gets the data for a given apiary"
  [apiary-uuid]
  (jdbc/query sql-db ["SELECT * FROM apiaries WHERE uuid = ? LIMIT 1" (ensure-uuid apiary-uuid)] :result-set-fn first))

(defn apiary-get-hives
  "Get all the hive UUIDs for an apiary"
  [apiary-uuid]
  (jdbc/query sql-db ["SELECT uuid FROM hives WHERE apiary_uuid = ?" (ensure-uuid apiary-uuid)]))

(defn apiary-create
  "Creates a new apiary"
  [{beekeeper-uuid :beekeeper_uuid :as parameters}]
  (let [clean-params (assoc parameters :beekeeper_uuid (ensure-uuid beekeeper-uuid))]
    (first (jdbc/insert! sql-db :apiaries clean-params))))
