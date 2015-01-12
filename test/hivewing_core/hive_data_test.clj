(ns hivewing-core.hive-data-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.hive-data :refer :all]))

(use-fixtures :each clean-database)

(deftest test-that-you-can-get-keys
  (let [{hive-uuid :hive-uuid
         worker-uuid :worker-uuid
         :as res}  (create-worker)]

      (hive-data-push hive-uuid worker-uuid "worker-key-1" "boo")
      (hive-data-push hive-uuid worker-uuid "worker-key-2" "boo")
      (hive-data-push hive-uuid nil "hive-key-1" "boo")
      (hive-data-push hive-uuid nil "hive-key-2" "boo")

      (is (= 2 (count (hive-data-get-keys hive-uuid))))
      (is (= '("hive-key-1" "hive-key-2") (sort (hive-data-get-keys hive-uuid))))
      (is (= 2 (count (hive-data-get-keys hive-uuid worker-uuid))))
      (is (= '("worker-key-1" "worker-key-2") (sort (hive-data-get-keys hive-uuid worker-uuid))))
      ))
(deftest test-that-you-can-get-data
  (let [{hive-uuid :hive-uuid
         worker-uuid :worker-uuid
         :as res}  (create-worker)]

      (doseq [x (range 0 (+ hivewing-core.hive-data/hive-data-keep-count 10))]
        (hive-data-push hive-uuid nil "hive-key-1" (str x))
        (hive-data-push hive-uuid worker-uuid "worker-key-1" (str x)))

      (let [hive (hive-data-read hive-uuid nil "hive-key-1")
            worker (hive-data-read hive-uuid worker-uuid "worker-key-1")]
        (= hivewing-core.hive-data/hive-data-keep-count (count hive))
        (= hivewing-core.hive-data/hive-data-keep-count (count worker))
        )))
