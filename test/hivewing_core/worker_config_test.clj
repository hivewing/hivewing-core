(ns hivewing-core.worker-config-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :as helpers]
            [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.postgres-json]
            [clojure.java.jdbc :as jdbc]
            [hivewing-core.worker-config :refer :all]))

(use-fixtures :each helpers/clean-database)
(comment
  (def res (helpers/create-worker))
  (worker-config-get (:worker-uuid res))
  (worker-config-get (:worker-uuid res) :include-system-keys true)
  (worker-config-set (:worker-uuid res) {"apple" "saucerse"} )
  (worker-config-set (:worker-uuid res) {"apple" "other" "big" "boy" "paara" "sdsdfsdf"} )
  (worker-config-set (:worker-uuid res) {".system-config" 123} :allow-system-keys false)
  (worker-config-set (:worker-uuid res) {".system-config" 123} :allow-system-keys true)

  )
(deftest create-a-worker
  (testing "system names"
      (is (worker-config-system-name? ".system-name"))
      (is (not (worker-config-system-name? "not-a.system-name")))
    )
  (testing "valid names"
      (is (worker-config-valid-name? "applesauce.system-name"))
      (is (worker-config-valid-name? "123"))
      (is (worker-config-valid-name? ".applesauce.system-name"))
      (is (worker-config-valid-name? "applesauce.system-name"))
      (is (not (worker-config-valid-name? "!applesauce.system-name")))
    )
  (testing "can find workers which have a given config value"
      (let [{worker1-uuid :worker-uuid} (helpers/create-worker)
            {worker2-uuid :worker-uuid} (helpers/create-worker)
            {worker3-uuid :worker-uuid} (helpers/create-worker)
            image-url "http://s3.com/12345"]

          (worker-config-set worker1-uuid {".hive-image" image-url})
          (worker-config-set worker2-uuid {".hive-image" "http://s3.com/0000"})
          (worker-config-set worker3-uuid {".hive-image" image-url})
      ))
  (testing "can delete all of the worker data"
      (let [{worker-uuid :worker-uuid} (helpers/create-worker)]
          (worker-config-set worker-uuid {"1" 1})
          (worker-config-set worker-uuid {"2" 2})
          (worker-config-set worker-uuid {"3" 3})
          (is (not (empty? (worker-config-get worker-uuid))))
          (worker-config-delete worker-uuid)
          (is (empty? (worker-config-get worker-uuid)))
      )
    ))

(deftest worker-config-delete-field
  (let [{worker-uuid :worker-uuid} (helpers/create-worker)]
      (worker-config-set worker-uuid {"1" 1})
      (worker-config-set worker-uuid {"2" 2})
      (worker-config-set worker-uuid {"3" 3})
      (worker-config-set worker-uuid {"1" nil})
      (is (nil? (get (worker-config-get worker-uuid) "1")))))
(comment

  (def worker-uuid (:worker-uuid (helpers/create-worker)))
      (worker-config-set worker-uuid {".tasks.worker1" "running"} :allow-system-keys true)
      (worker-config-set worker-uuid {".tasks.worker2" "running"} :allow-system-keys true)
      (worker-config-set worker-uuid {".tasks.worker3" "stopped"} :allow-system-keys true)
      (println (jdbc/query sql-db
         ["SELECT * FROM worker_configs WHERE worker_uuid = ? AND key LIKE '.tasks.%'"
          worker-uuid]))
      (println (jdbc/query sql-db
         ["SELECT * FROM worker_configs WHERE worker_uuid = ? " worker-uuid]))
      (worker-config-get-tasks worker-uuid)
  )
(deftest worker-config-tasks
  (let [{worker-uuid :worker-uuid} (helpers/create-worker)]
      (worker-config-set worker-uuid {".tasks.worker1" "running"} :allow-system-keys true)
      (worker-config-set worker-uuid {".tasks.worker2" "running"} :allow-system-keys true)
      (worker-config-set worker-uuid {".tasks.worker3" "stopped"} :allow-system-keys true)
      (let [tasks (worker-config-get-tasks worker-uuid)]
        (is (= 3 (count tasks)))
        (is (= ["worker1" "worker2" "worker3"] (sort (keys tasks)))))))
