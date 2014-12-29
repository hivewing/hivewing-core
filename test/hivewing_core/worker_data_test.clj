(ns hivewing-core.worker-data-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [taoensso.carmine :as car ]
            [hivewing-core.worker-data :refer :all]))

(use-fixtures :each clean-database)

(deftest worker-data-get-and-set
  (testing "worker-data-get-and-set"
    (let [worker-uuid "123"
          field-name :apples
          stored-data (worker-data-store worker-uuid (list field-name "DATA"))
          read-data   (worker-data-read worker-uuid field-name)]
        (is (= (first (vals stored-data)) (:value (first read-data))))
        ))
  )
