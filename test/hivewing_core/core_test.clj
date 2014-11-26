(ns hivewing-core.core-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.core :refer :all]))
(use-fixtures :each clean-database)

;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))
