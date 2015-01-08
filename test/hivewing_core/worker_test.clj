(ns hivewing-core.worker-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.worker-config :refer :all]
            [hivewing-core.worker :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.hive-logs :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(use-fixtures :each clean-database)

(comment
  (def beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"})))
  (def apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid})))
  (def worker-uuid    (:uuid (worker-create {:apiary_uuid apiary-uuid})))
  (def worker (worker-get worker-uuid))

  )
(deftest find-a-worker-with-invalid-uuid
  (is (not (worker-get "123"))))

(deftest reset-a-worker-token
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          worker-uuid    (:uuid (worker-create {:apiary_uuid apiary-uuid}))
          prior-access   (:access_token (worker-get worker-uuid :include-access-token true))
          new-worker     (worker-reset-access-token worker-uuid)
          new-access     (:access_token (worker-get worker-uuid :include-access-token true))
          ]
      (is (not (= "unnamed" (:name (worker-get worker-uuid)))))
      (is (:name (worker-get worker-uuid)))
      (is (not (reduce = (map str [prior-access new-access]))))
        ))
(deftest create-a-worker-with-name
  (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
        apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
        worker         (worker-create {:name "foobar" :apiary_uuid apiary-uuid})
        worker-get-res (worker-get (:uuid worker))]
    (println worker-get-res)
    (is (= "foobar" (:name worker-get-res)))
  ))

(deftest create-with-blank-name-autogen
  (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
        apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
        worker         (worker-create {:name "" :apiary_uuid apiary-uuid})
        worker-get-res (worker-get (:uuid worker))]
    (is (not (= "" (:name worker-get-res))))))

(deftest updating-worker-name
  (let [{worker-uuid :worker-uuid } (create-worker)
        worker-name (:name (worker-get worker-uuid))
                           ]
    (worker-set-name worker-uuid "new-name")
    (is (= "new-name" (:name (worker-get worker-uuid))))))


(deftest deleting-a-worker-via-worker-list
  (let [{apiary-uuid :apiary-uuid hive-uuid :hive-uuid} (create-worker)]

    (do
      (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
      (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
      (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
      (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
      (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid}))

    (doseq [{worker-uuid :uuid} (worker-list hive-uuid :page 1 :per-page 100)]
      (worker-delete worker-uuid))
    (is (empty? (worker-list hive-uuid :page 1 :per-page 100)))
  ))

(deftest delete-a-worker-test
  (let [{worker-uuid :worker-uuid hive-uuid :hive-uuid} (create-worker)]
      (worker-config-set worker-uuid {".tasks.worker1" "running"} :allow-system-keys true)
      (worker-config-set worker-uuid {".tasks.worker2" "running"} :allow-system-keys true)
      (worker-config-set worker-uuid {".tasks.worker3" "stopped"} :allow-system-keys true)
      (hive-logs-push hive-uuid worker-uuid nil "System log!")

      (worker-delete worker-uuid)))
