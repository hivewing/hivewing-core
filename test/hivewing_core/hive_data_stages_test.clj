(ns hivewing-core.hive-data-stages-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.hive-data-stages :refer :all])
  )

(use-fixtures :each clean-database)

(deftest get-stages
  (is (stages)))
