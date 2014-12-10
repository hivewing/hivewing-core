(ns hivewing-core.public-keys-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.beekeeper :as bk]
            [clojure.java.jdbc :refer :all]
            [hivewing-core.public-keys :as pk]))

(use-fixtures :each clean-database)

(deftest test-public-keys-create
    (let [
          beekeeper-uuid (:uuid (bk/beekeeper-create {:email "my_email@example.com"}))
          public-key     (pk/public-key-create beekeeper-uuid "123DATA")
          public-keys    (pk/public-keys-for-beekeeper beekeeper-uuid)
          ]
      (is (= (:uuid public-key) (:uuid (first public-keys))))
      ))

(deftest test-public-keys-delete
    (let [
          beekeeper-uuid (:uuid (bk/beekeeper-create {:email "my_email@example.com"}))
          public-key     (pk/public-key-create beekeeper-uuid "123DATA")
          public-keys    (pk/public-keys-for-beekeeper beekeeper-uuid)
          ]
      (is (not (empty? public-keys)))
      (pk/public-keys-delete beekeeper-uuid)
      (is (empty? (pk/public-keys-for-beekeeper beekeeper-uuid)))
      ))
