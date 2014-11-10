(ns hivewing-core.worker
  (:require [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]
            [environ.core  :refer [env]]))

;(jdbc/query sql-db ["SELECT * FROM workers"])
