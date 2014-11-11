(ns hivewing-core.beekeeper-test
  (:require [clojure.test :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(deftest create-a-beekeeper
  (testing "create with a basic email address"
    (let [create-result (beekeeper-create {:email "my_email@example.com"})
          get-result (beekeeper-get (get create-result :uuid))]
        (is get-result)
        (is (= (get create-result :email)
               (get get-result    :email)))))
  (testing "create with a without a basic email address"
    (let [result (try
                    (beekeeper-create {:email nil})
                    (catch org.postgresql.util.PSQLException e (str "error")))]
      (is (= "error" result)))))
