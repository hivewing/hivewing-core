(ns hivewing-core.worker-test
  (:require [clojure.test :refer :all]
            [hivewing-core.worker :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

;(def beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"})))
;(def apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid})))
;(def hive-uuid      (:uuid (hive-create {:apiary_uuid apiary-uuid})))
;(def worker  (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid}))

(deftest create-a-worker
  (quote testing "create validly"
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-uuid      (:uuid (hive-create {:apiary_uuid apiary-uuid}))
          worker-result  (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
          worker-retrieval  (worker-get (:uuid worker-result))]
        (is worker-result)
        (is worker-retrieval)
        ))

  (testing "reset token"
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          worker-uuid    (:uuid (worker-create {:apiary_uuid apiary-uuid}))
          prior-access   (:access_token (worker-get worker-uuid :include-access-token true))
          new-worker     (worker-reset-access-token worker-uuid)
          new-access     (:access_token (worker-get worker-uuid :include-access-token true))
          ]
      (is (not (reduce = (map str [prior-access new-access]))))
        )))
