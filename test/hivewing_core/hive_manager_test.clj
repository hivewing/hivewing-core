(ns hivewing-core.hive-manager-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.beekeeper :as bk]
            [hivewing-core.hive-manager :as hm]))

(use-fixtures :each clean-database)

(deftest list-and-delete-hive-managers
    (let [{bk-uuid :beekeeper-uuid hive-uuid :hive-uuid} (create-worker)]
      (doseq [i [1 2 3 4 5 6 7 8 9]]
        (let [email   (str i "@gmail.com")
              bk (bk/beekeeper-create {:email email})]
        (hm/hive-manager-create hive-uuid (:uuid bk))))

        (is (not (empty? (hm/hive-managers-get hive-uuid))))
        (doseq [{hive-manager-uuid :uuid} (hm/hive-managers-get hive-uuid)]
            (hm/hive-manager-delete hive-manager-uuid))
        (is (empty? (hm/hive-managers-get hive-uuid)))))
