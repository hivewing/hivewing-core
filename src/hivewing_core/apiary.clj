(ns hivewing-core.apiary
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn apiary-get
  "Gets the data for a given apiary"
  [apiary-uuid]
  (jdbc/query sql-db ["SELECT * FROM apiaries WHERE uuid = ? LIMIT 1" (ensure-uuid apiary-uuid)] :result-set-fn first))

(defn apiary-create
  "Creates a new apiary"
  [parameters]
  (first (jdbc/insert! sql-db :apiaries parameters)))
