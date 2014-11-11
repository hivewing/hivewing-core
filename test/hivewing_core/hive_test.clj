(ns hivewing-core.hive-test
  (:require [clojure.test :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(deftest create-a-hive
  (testing "create validly"
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-result    (hive-create {:apiary_uuid apiary-uuid})
          hive-retrieval (hive-get (:uuid hive-result))]
        (is hive-result)
        (is hive-retrieval)
        (is (= (map str (:apiary_uuid hive-result) apiary-uuid))
        ))))
