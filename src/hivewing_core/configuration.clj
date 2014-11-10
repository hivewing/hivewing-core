(ns hivewing-core.configuration
  (:require [environ.core  :refer [env]]))

(def aws-credentials
  "The AWS Credentials to connect to ddb"
  {:access-key (env :hivewing-aws-access-key),
   :secret-key (env :hivewing-aws-secret-key),
   :endpoint   (env :hivewing-ddb-endpoint)})

(def sql-db
  "The SQL db connection parameters"
  {
    :subprotocol "postgresql"
    :subname (env :hivewing-sql-connection-string)
  })
