(ns hivewing-core.worker-data-test
  (:require [clojure.test :refer :all]
            [taoensso.carmine :as car ]
            [hivewing-core.worker-data :refer :all]))

(deftest worker-data-get-and-set
  (testing "worker-data-get-and-set"
    (redis (car/flushall))
    (let [worker-uuid "123"
          field-name :apples
          stored-data (worker-data-store worker-uuid field-name "DATA")
          read-data   (worker-data-read worker-uuid field-name)]
        (is (= (keys stored-data) (keys read-data)))
        (is (= (first (vals stored-data)) (first (keys (first (vals read-data))))))
        ))
  )
