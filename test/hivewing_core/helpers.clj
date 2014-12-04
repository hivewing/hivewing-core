(ns hivewing-core.helpers
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.carmine :as car ]
            [hivewing-core.hive-manager :refer :all]
            [hivewing-core.configuration :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.hive-image :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.worker :refer :all]
            [hivewing-core.worker-config :refer :all]
            [hivewing-core.pubsub :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(defn clean-database [test-function]
  ; Clear out the DDB tables.
  (worker-ensure-tables :delete_first)

  ; Clear the hive-images
  (ensure-hive-image-bucket :delete_first)
  (redis (car/flushall))

  (jdbc/execute! sql-db ["TRUNCATE TABLE beekeepers, apiaries, hive_managers, hives, workers, public_keys"])
  (test-function))

(defn create-worker
  []
  (let [
          beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-uuid      (:uuid (hive-create {:apiary_uuid apiary-uuid}))
          hive-manager-uuid (:uuid   (hive-manager-create hive-uuid beekeeper-uuid))
          worker-result  (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
          worker-retrieval  (worker-get (:uuid worker-result))
          ]
      (hash-map  :worker-uuid (:uuid worker-retrieval)
                 :beekeeper-uuid beekeeper-uuid
                 :apiary-uuid apiary-uuid
                 :hive-uuid hive-uuid
                 :hive-manager-uuid hive-manager-uuid)))
