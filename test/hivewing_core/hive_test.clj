(ns hivewing-core.hive-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(use-fixtures :each clean-database)

(deftest create-a-hive
  (testing "create validly"
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-result    (hive-create {:apiary_uuid apiary-uuid})
          hive-retrieval (hive-get (:uuid hive-result))]
        (is hive-result)
        (is hive-retrieval)
        (is (= (map str (:apiary_uuid hive-result) apiary-uuid)))
        (is (:name hive-retrieval))
        (is (not (= "unnamed" (:name hive-retrieval)))
        ))))
(deftest delete-a-hive
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-result    (hive-create {:apiary_uuid apiary-uuid})
          hive-retrieval (hive-get (:uuid hive-result))]
        (is hive-result)
        (is (hive-delete (:uuid hive-result)))
        (is (not (hive-get (:uuid hive-result))))
        ))
