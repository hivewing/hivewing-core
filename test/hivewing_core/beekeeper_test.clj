(ns hivewing-core.beekeeper-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(use-fixtures :each clean-database)

(deftest create-a-beekeeper
  (testing "create with a basic email address"
    (let [create-result (beekeeper-create {:email "my_email@example.com"})
          get-result (beekeeper-get (get create-result :uuid))]
        (is get-result)
        (is (= (get create-result :email)
               (get get-result    :email)))))
  (testing "delete a bk"
    (let [create-result (beekeeper-create {:email "my_email@example.com"})
          {bk-uuid :uuid} (beekeeper-get (get create-result :uuid))]
        (is (beekeeper-delete bk-uuid))
        (is (not (beekeeper-get bk-uuid)))
      ))
  (testing "create with a without a basic email address"
    (let [result (try
                    (beekeeper-create {:email nil})
                    (catch org.postgresql.util.PSQLException e (str "error")))]
      (is (= "error" result)))))
