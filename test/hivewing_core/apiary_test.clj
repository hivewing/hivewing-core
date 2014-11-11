(ns hivewing-core.apiary-test
  (:require [clojure.test :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(deftest create-an-apiary
  (testing "create with a beekeeper"
    (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-result (apiary-create {:beekeeper_uuid beekeeper-uuid}) ]
        (is apiary-result)
        (is (= (:beekeeper_uuid beekeeper-uuid)))
        ))
  )
