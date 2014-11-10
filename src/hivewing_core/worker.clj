(ns hivewing-core.worker
  (:require [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]
            [environ.core  :refer [env]]))

;(jdbc/insert! sql-db :workers {:name "first worker"})
;(jdbc/query sql-db ["SELECT * FROM workers"])
;(jdbc/update! sql-db :workers {:name "New name"} ["uuid = ?" "fc84da1e-692c-11e4-a377-5f03441b19f7"])
