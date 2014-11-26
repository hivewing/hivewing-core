(ns hivewing-core.helpers
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.carmine :as car ]
            [hivewing-core.configuration :refer :all]
            [hivewing-core.pubsub :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(defn clean-database [test-function]
  (test-function)
  (redis (car/flushall))
  (jdbc/execute! sql-db ["TRUNCATE TABLE beekeepers, apiaries, hive_managers, hives, workers, public_keys"]))
