(ns hivewing-core.hive-data-processing-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer [<!]]
            [hivewing-core.hive-data :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.hive-data-processing :refer :all])
  )

(use-fixtures :each clean-database)


(comment

    (def hu "12345678-1234-1234-1234-123456789012")
    (def pipeline-desc (hive-data-stages-index hu))
    (hive-data-get-keys hu)
    (hive-data-read hu nil "worker-data-avg")
    (doseq [x (range 0 60)]
      (Thread/sleep 500)
      (hive-data-push-to-processing hu "worker-uuid" "data" x (java.util.Date.))
      (hive-data-push-to-processing hu nil "hive-data" x (java.util.Date.))
      )
    )

(deftest test-get-firehose
  (let [channel (drink-the-firehose!)]
    (is channel)

    (hive-data-push-to-processing "h" "w" "data" 1 (java.util.Date.))
    (hive-data-push-to-processing "h" nil "hive-data" 1 (java.util.Date.))
    (hive-data-push-to-processing "h" "w2" "data" 1 (java.util.Date.))

    (async/take! channel
      (fn [msg]
        (is (= (:worker-uuid msg) "w"))
        (is (= (:hive-uuid msg) "h"))
        (is (= (:data-name msg) "data"))
        (is (= (:data-value msg) 1)))
      )))
