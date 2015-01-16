(ns hivewing-core.configuration
  (:require
    [amazonica.core :as aws]
    [amazonica.aws.sqs :as sqs]
    [environ.core  :refer [env]]))

(def aws-credentials
  "The AWS Credentials to connect to AWS"
  {:access-key (env :hivewing-aws-access-key),
   :secret-key (env :hivewing-aws-secret-key)}
  )

(def ses-aws-credentials
  "The credentials to connect to SES"
  (assoc aws-credentials :endpoint   (env :hivewing-sqs-endpoint)))

(def sqs-aws-credentials
  (assoc aws-credentials :endpoint   (env :hivewing-sqs-endpoint)))

(def s3-aws-credentials
  (assoc aws-credentials :endpoint   (env :hivewing-s3-endpoint)))

(def sql-db
  "The SQL db connection parameters"
  {
    :subprotocol "postgresql"
    :subname (env :hivewing-sql-connection-string)
  })
