(ns hivewing-core.worker-config-test
  (:require [clojure.test :refer :all]
            [hivewing-core.worker-config :refer :all]))

(deftest create-a-worker
  (testing "system names"
      (is (worker-config-system-name? ".system-name"))
      (is (not (worker-config-system-name? "not-a.system-name")))
    )
  (testing "valid names"
      (is (worker-config-valid-name? "applesauce.system-name"))
      (is (worker-config-valid-name? "123"))
      (is (worker-config-valid-name? ".applesauce.system-name"))
      (is (worker-config-valid-name? "applesauce.system-name"))
      (is (not (worker-config-valid-name? "!applesauce.system-name")))
    ))
