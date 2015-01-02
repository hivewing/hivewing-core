(ns hivewing-core.hive-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(use-fixtures :each clean-database)

(comment
  (def beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"})))
  (def apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid})))
  (def uuid            "12345678-1234-1234-1234-123456789012")
  hive-result    (hive-create {:uuid uuid :apiary_uuid apiary-uuid})

  )
(deftest find-a-hive-with-invalid-uuid
  (is (not (hive-get "123"))))

(deftest create-a-hive-with-specific-uuid
  (let [beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
        apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
        uuid            "12345678-1234-1234-1234-123456789012"
        hive-result    (hive-create {:uuid uuid :apiary_uuid apiary-uuid})
        hive-retrieval (hive-get (:uuid hive-result))]
      (is (= uuid (:uuid hive-result)))
      ))

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

(deftest update-a-hive-image-url
  (let [res (create-worker)
        hive-uuid (:hive-uuid res)]
      (hive-update-hive-image-url hive-uuid "THE-url")))
