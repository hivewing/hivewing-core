(ns hivewing-core.worker-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.worker :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(use-fixtures :each clean-database)

(comment
  (def beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"})))
  (def apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid})))
  (def worker-uuid    (:uuid (worker-create {:apiary_uuid apiary-uuid})))
  (def worker (worker-get worker-uuid))

  )

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
        worker         (worker-create {:name "foobar" :apiary_uuid apiary-uuid})]
        worker-get     (worker-get (:uuid worker))

    (is (= "foobar" (:name worker-get)))
  ))

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
