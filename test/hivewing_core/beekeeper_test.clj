(ns hivewing-core.beekeeper-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.beekeeper :refer :all]))

(use-fixtures :each clean-database)
(comment
    (def create-result (beekeeper-create {:email "my_email@example.com"}))
    (def get-result (beekeeper-get (get create-result :uuid)))
    (def uuid (:uuid create-result))
    (def res (beekeeper-set-password uuid "mypasswordA1"))
    (def get-all-result (beekeeper-get uuid :include-all-fields))
      (beekeeper-set-password uuid "PasswordIsGood!")
      (is (not (beekeeper-validate (:email create-result) "GasswordIsGood!")))
      (is (beekeeper-validate (:email create-result) "PasswordIsGood!"))
)

(deftest create-a-beekeeper
  (testing "beekeeper password"
    (let [create-result (beekeeper-create {:email "my_email@example.com"})
          get-result (beekeeper-get (get create-result :uuid))
          uuid (:uuid create-result)]
      (is (:email get-result))
      (try
        (beekeeper-set-password uuid "mypassword")
        (is false "Password is invlaid!")
        (catch Exception e ))
      (beekeeper-set-password uuid "PasswordIsGood!")
      (is (not (beekeeper-validate (:email create-result) "GasswordIsGood!"))
      (is (beekeeper-validate (:email create-result) "PasswordIsGood!"))
    )))
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
