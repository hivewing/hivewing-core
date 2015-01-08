(ns hivewing-core.worker-events-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [taoensso.carmine :as car ]
            [hivewing-core.worker-events :refer :all]))

(use-fixtures :each clean-database)

(deftest test-worker-events-send
  (worker-events-send "123" :event :value))

(deftest test-worker-events-send-reboot
  (worker-events-send-reboot "123"))

(deftest test-worker-events-send-reset
  (worker-events-send-reset "123"))
