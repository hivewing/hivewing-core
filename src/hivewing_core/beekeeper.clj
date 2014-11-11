(ns hivewing-core.beekeeper
  (:require [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]))

(defn beekeeper-get
  "Get the information for a given beekeeper"
  [beekeeper-uuid]
  (jdbs/query sql-db ["SELECT * FROM beekeepers WHERE uuid = ?" beekeeper-uuid]))

(defn beekeeper-create
  "Create a new beekeeper"
  [email & parameters]
  (println "TODO"))
