(ns hivewing-core.hive-log-test
  (:require [clojure.test :refer :all]
            [conjure.core :as conjure]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.configuration :as config]
            [hivewing-core.hive :refer :all]
            [hivewing-core.hive-logs :refer :all]
            [hivewing-core.apiary :refer :all]))

(use-fixtures :each clean-database)
(comment
  (java.util.Date.)
  )

(deftest hive-logs-adding-some-logs
  (let [created (create-hive)
        hive-uuid (:hive-uuid created)]
    (hive-logs-push hive-uuid "worker-uuid" "task-name" (java.util.Date.) "Log message")
    (is (= 1 (count (hive-logs-read hive-uuid))))
    ))
