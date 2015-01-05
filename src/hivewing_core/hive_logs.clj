(ns hivewing-core.hive-logs
  (:require
            [taoensso.timbre :as logger]
            [amazonica.aws.simpledb :as sdb]
            [digest :as digest]
            [hivewing-core.configuration :refer [simpledb-aws-credentials]]
            [environ.core  :refer [env]]))
(comment
(sdb/create-domain simpledb-aws-credentials :domain-name "domain")
(sdb/list-domains simpledb-aws-credentials )
(sdb/put-attributes simpledb-aws-credentials  :domain-name "domain"
                    :item-name "my-item"
                    :attributes [{:name "foo"
                                  :value "bar"}
                                 {:name "baz"
                                  :value 42}])
(sdb/select simpledb-aws-credentials :select-expression
            "select * from domain where baz = '42' ")



  (def domain (sdb/create-domain simpledb-aws-credentials :domain-name "123"))
  (sdb/create-domain simpledb-aws-credentials :domain-name "hive-logs-c6e439c0-9507-11e4-9c42-0242ac110285")
  (sdb/list-domains simpledb-aws-credentials)
  (.nextInt (java.util.Random.) 999)
  (.getTime (java.util.Date.))
  (joda-time/to-millis-from-epoch (joda-time/now))
  (string-chunk string 10)
  (hive-logs-push "1" "worker" "task" (java.util.Date.) "log message")
  (sdb/select simpledb-aws-credentials :select-expression "SELECT * FROM hive-logs-1")

(subs string 0 100)
  (def string "123123123123123123123123123123123123123123123")
  (split-at 10 string)
  (slice 10 string)
  (subs string 0 10)
  (reduce #(1 (take 10 %2)) string)
  (macroexpand '(hive-logs-with-domain-name "fooo" (println "has"))))

(defmacro hive-logs-with-domain-name
  [domain-name & body]
    `(try
        ~@body
        (catch com.amazonaws.AmazonServiceException e#
          (do
            (logger/info "Creating hive-logs domain " ~domain-name)
            (sdb/create-domain simpledb-aws-credentials :domain-name ~domain-name)
              ~@body)))
  )

(defn hive-logs-read
  "Read hive logs, with a simple filter available.
  You can read logs sequentially, filtered by hive, worker (maybe)
  and given a starting timestamp, it will return up to cnt records."
  [hive-uuid & args]
    (try
      (let [args (apply hash-map args)
            start-at (or (:start-at args) (java.util.Date.))
            domain-name (hive-logs-domain-name hive-uuid)
            worker-uuid (:worker-uuid args)
            worker-uuid-str (if worker-uuid (str " AND worker-uuid = " worker-uuid) "")
            task-name (:task-name args)
            task-name-str (if task-name (str " AND task = " task-name) "")
            hive-uuid (str hive-uuid)
            select-str (str "select * from " domain-name
                         " where  at >= \"" (.getTime start-at) "\" "
                         worker-uuid-str
                         task-name-str
                         " ORDER BY at DESC LIMIT 200")
            ]
          (logger/info "Looking up hive logs " select-str)
          (sdb/select simpledb-aws-credentials :select-expression select-str)
          )
      (catch Exception e (do (println "error!" e) (logger/error (str e)) []))))

(defn- log-message-chunk
  "Chunks a log message into separate parts"
  ([s len] (string-chunk s len []))
  ([s len chunks]
    (if (<= (count s) len)
      (conj chunks s)
      (recur (subs s len) len (conj chunks (subs s 0 len))))))

(defn hive-logs-domain-name
  [hive-uuid]
    (clojure.string/replace (str "hive-logs-" hive-uuid) #"-" ""))

(defn hive-logs-item-name
  [worker-uuid task-name timestamp message]
    (str (.getTime timestamp) "-" worker-uuid "-" task-name "-" (.nextInt (java.util.Random. (.getTime timestamp)) 999999)))

(defn hive-logs-push
  "Push some hive logs into simple db.
  Given a hive / worker (optional) - and task name.
  If there is no task name, it is considered a 'system-log'
  "
  [hive-uuid worker-uuid task timestamp message]
    ;; Split into chunks of 999 bytes
    (let [domain-name (hive-logs-domain-name hive-uuid)
          chunks (log-message-chunk message 1000)]
      (doseq [msg-chunk chunks]
        (hive-logs-with-domain-name domain-name
          (sdb/put-attributes simpledb-aws-credentials
                          :domain-name  domain-name
                          :item-name (hive-logs-item-name worker-uuid task timestamp msg-chunk)
                          :attributes [ {:name :worker-uuid :value (str worker-uuid)}
                                        {:name :task   :value task}
                                        {:name :log    :value msg-chunk}
                                        {:name :at     :value (.getTime timestamp)}
                                        ])
              ))))
