(ns hivewing-core.public-keys-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.beekeeper :refer :all]
            [clojure.java.jdbc :refer :all]
            [hivewing-core.public-keys :refer :all]))

(use-fixtures :each clean-database)

(deftest public-keys
  (testing "create"
    (let [
          beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          public-key     (public-key-create beekeeper-uuid "123DATA")
          public-keys    (public-keys-for-beekeeper beekeeper-uuid)
          ]
      (is (= (:uuid public-key) (:uuid (first public-keys))))
      )))
