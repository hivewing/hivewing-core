(ns hivewing-core.apiary
  (:require [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]))

(defn apiary-get
  "Gets the data for a given apiary"
  [apiary-uuid]
  (jdbc/query sql-db ["SELECT * FROM apiaries WHERE uuid = ?" apiary-uuid]))

(defn apiary-create
  "Creates a new apiary"
  [beekeeper-uuid & parameters]
  (println "TODO"))
