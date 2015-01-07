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
    (def res (create-hive))
    (def hive-uuid (:hive-uuid res))
    (hive-logs-push hive-uuid nil "atask-name" "LOG")
    (hive-logs-read hive-uuid)
  )

(deftest hive-logs-adding-some-logs
  (let [created (create-hive)
        hive-uuid (:hive-uuid created)]
    (hive-logs-push hive-uuid nil "task-name" "Log message")
    (println (hive-logs-read hive-uuid))))

(deftest hive-logs-adding-some-logs-finding-by-task
  (let [created (create-worker)
        hive-uuid (:hive-uuid created)
        worker-uuid (:worker-uuid created)]
    (hive-logs-push hive-uuid worker-uuid "task-name1" "Log1")
    (hive-logs-push hive-uuid worker-uuid "task-name2" "Log2")
    (hive-logs-push hive-uuid worker-uuid "task-name3" "Log3")
    (hive-logs-push hive-uuid nil nil "LogAll")
    (let [res (hive-logs-read hive-uuid :task "task-name1" )]
      (is (=  1 (count res)))
      (is (= "Log1" (:message (first res)))))
    (let [res (hive-logs-read hive-uuid :task "task-name2" )]
      (is (=  1 (count res)))
      (is (= "Log2" (:message (first res)))))
    (let [res (hive-logs-read hive-uuid)]
      (is (=  4 (count res))))
    (let [res (hive-logs-read hive-uuid :worker-uuid worker-uuid)]
      (is (=  3 (count res))))
    (let [res (hive-logs-read hive-uuid :task "task-name2" )]
      (is (=  1 (count res)))
      (is (= "Log2" (:message (first res)))))
    ))
