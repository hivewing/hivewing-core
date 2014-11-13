(ns hivewing-core.beekeeper
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn beekeeper-get
  "Get the information for a given beekeeper"
  [beekeeper-uuid]
  (jdbc/query sql-db ["SELECT * FROM beekeepers WHERE uuid = ? LIMIT 1" (ensure-uuid beekeeper-uuid)] :result-set-fn first))

(defn beekeeper-create
  "Create a new beekeeper"
  [parameters]
  ; it returns a vector, we want the first value.
  (first (jdbc/insert! sql-db :beekeepers parameters)))
