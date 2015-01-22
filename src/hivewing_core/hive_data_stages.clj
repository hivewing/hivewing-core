(ns hivewing-core.hive-data-stages
  (:require
            [taoensso.timbre :as logger]
            [clj-time.core :as ctime]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.postgres-json :as pgjson]
            [hivewing-core.configuration :refer [sql-db]]
            [clojure.java.jdbc :as jdbc]))

(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (if (number? s)
    s
    (if (re-find #"^-?\d+\.?\d*$" s)
      (read-string s))))

(def message-format {
  :hive-uuid   "(always present)"
  :worker-uuid "(may be present / not)"
  :data-name   "(string)"
  :data-value  "(string)"
  :at          "epoch ts or something"
  })

(defn get-push-worker-uuid
  [stage-def msg]
  (let [
        in-worker-selector (first (keys (:in stage-def)))
        in-data-name       (first (vals (:in stage-def)))
        out-worker-selector (first (keys (:out stage-def)))
        ]

    (case out-worker-selector
      :worker (str (:worker-uuid msg))
      :hive nil)))

(defn get-push-data-name
  [stage-def]
  (first (vals (:out stage-def))))

(defn get-stream-id
  [stage-def msg]
  ;;
  ;; Cases:
  ;;    :worker => :worker EASY
  ;;    :worker => :hive   COMBINING
  ;;    :hive   => :hive   EASY
  ;;

  (let [
        in-worker-selector (first (keys (:in stage-def)))
        in-data-name       (first (vals (:in stage-def)))
        out-worker-selector (first (keys (:out stage-def)))
        ]
    (case in-worker-selector
      :worker
        (if (= :hive out-worker-selector)
          (str "all-workers:" in-data-name)
          (str (:worker-uuid msg) ":" in-data-name))
      :hive (str "hive:" in-data-name))))

(defn compare-values
  [test-func data-value baseline-value]
  ;; Compare as numbers if both parse as numbers
    (let [data-value-i (parse-number data-value)
          baseline-i   (parse-number baseline-value)
          test-func (case test-func
                :gt  >
                :gte >=
                :lt  <
                :lte <=
                :eq  =
                "default" =)]
      (if (and data-value-i baseline-i)
          (test-func data-value-i baseline-i)
          (test-func 0 (compare data-value baseline-value)))))

(defn push-post-alert
  "Push an alert - to the system.
  Can usually be an email of POST hook"
  [url hive-uuid worker-uuid data-name data-value baseline test-func]
  (logger/info "TODO POST ALERT:" hive-uuid worker-uuid data-name data-value baseline test-func))

(defn push-email-alert
  "Push an alert - to the system.
  Can usually be an email of POST hook"
  [email-addr hive-uuid worker-uuid data-name data-value baseline test-func]
  (logger/info "TODO EMAIL ALERT:" hive-uuid worker-uuid data-name data-value baseline test-func))

(defn push-data
  "Push the message out (back into Kinesis really)
  But also through the SQL database, so you can read it
  from the API"
  [hive-uuid worker-uuid data-name data-value]
    (logger/info "TODO Sending this out: " hive-uuid worker-uuid data-name data-value))


(defn alert-email-stage
  ([] {:type       :alert-email
       :description "Send an email to an address when a test condition is triggered by a data record"
       :params {
         :in         [:data-stream "The data records to test for the alert"]
         :email      [:email   "The email that the hook will hit"]
         :value      [:string "The value we are testing against"]
         :test       [[:gt :gte :lt :lte :eq] "The comparator"]
         }})
  ([stage-def]
    (let [baseline-value (:value stage-def)
          email          (:email stage-def)]

      (fn [x]
        (if (compare-values (:test stage-def) (:data-value x) baseline-value)
          (push-email-alert email
                             (:hive-uuid x)
                             (:worker-uuid x)
                             (:data-name x)
                             (:data-value x)
                             baseline-value
                             (:test stage-def)))))))
(defn alert-post-stage
  ([] {:type       :alert-post
       :description "POST to an url when a test condition is triggered by a data record"
       :params {
         :in         [:data-stream "The data records to test for the alert"]
         :url        [:url   "The URL that the POST hook will hit"]
         :value      [:string "The value we are testing against"]
         :test       [[:gt :gte :lt :lte :eq] "The comparator"]
         }})
  ([stage-def]
    (let [baseline-value (:value stage-def)
          url             (:url stage-def)]
      (fn [x]
        (if (compare-values (:test stage-def) (:data-value x) baseline-value)
            (push-post-alert url
                             (:hive-uuid x)
                             (:worker-uuid x)
                             (:data-name x)
                             (:data-value x)
                             baseline-value
                             (:test stage-def)))))))

(defn changed-email-stage
  ([] {:description "Send an email to an address when the values in the data stream change"
       :type       :change-email
       :params {
         :in         [:data-stream "The data records to test for the change"]
         :email      [:email   "The email that the hook will hit"]
         }})
  ([stage-def]
    (let [last-value     (atom nil)
          email          (:email stage-def)]

      (fn [x]
        (if (not (= @last-value (:data-value x)))
          (push-email-alert email
                             (:hive-uuid x)
                             (:worker-uuid x)
                             (:data-name x)
                             (:data-value x)
                             @last-value
                             :changed))
        (reset! last-value (:data-value x))))))

(defn changed-post-stage
  ([] {:type       :change-post
       :description "POST to an url when the data record is different from the previous one"
       :params {
         :in         [:data-stream "The data records to test for the change"]
         :url        [:url   "The URL that the POST hook will hit"]
         }})
  ([stage-def]
    (let [last-value     (atom nil)
          url          (:url stage-def)]

      (fn [x]
        (if (not (= @last-value (:data-value x)))
          (push-post-alert   url
                             (:hive-uuid x)
                             (:worker-uuid x)
                             (:data-name x)
                             (:data-value x)
                             @last-value
                             :changed))
        (reset! last-value (:data-value x))))))

(defn average-stage
  ([] {
       :type       :average
       :description "Average the stream of data records and emit the result to a new data stream"
       :params {
         :in         [:data-stream "The data records to feed into the average calculations"]
         :out        [:data-stream "Name of the output field the average should go to"]
         :window     [:integer "How long to wait between averages (in ms)"]
         }})
  ([stage-def]
    (let [start-state      {:sum 0 :cnt 0 :avg 0}
          window           (or (:window stage-def) 5000)
          output-target    (:out stage-def)
          ;; Average needs to hold values for ALL the inputs
          state            (atom {})
          last-calculation (atom {})
          ]
      (fn [x]
        (let [incr-val (or (parse-number (:data-value x)) 0)
              now (.getTime (java.util.Date.))
              stream-id  (get-stream-id stage-def x)
              stream-timer (get (swap! last-calculation (fn [curr]
                                                        (if (get curr stream-id)
                                                         curr
                                                         (assoc curr stream-id (.getTime (java.util.Date.)))))) stream-id)
              wait-until (+ stream-timer window)
              new-state (get (swap! state (fn [current]

                          (let [stream (or
                                         (get current stream-id)
                                         start-state)
                                sum (+ incr-val (:sum stream))
                                cnt (+ (:cnt stream) 1)
                                avg (/ (+ incr-val (* (:avg stream) (:cnt stream)))
                                       (+ 1 (:cnt stream)))]
                            (assoc current stream-id {:sum sum
                                                      :cnt cnt
                                                      :avg avg})))) stream-id)
            new-avg (:avg new-state)]

          (if (>= now wait-until)
            (do
              (push-data (:hive-uuid x)
                         (get-push-worker-uuid stage-def x)
                         (get-push-data-name stage-def)
                         new-avg)
              (swap! state assoc stream-id start-state))))))))

(defn log-stage
  ([] {:type       :log
       :description "Logging periodically that we received X records"
       :hidden     true
       :parms {
         :in         [:data-stream "The data records to log"]
         :count-rate [:integer "How often you should emit that you have recvd messages"]
         }})
  ([stage-def]
    (let [stage-count (or (:count-rate stage-def) 1)
          cnt (atom 0)
          ]
      (fn [x]
        (swap! cnt inc)
        (if (= 0 (mod @cnt stage-count))
          (logger/info "processed" @cnt "since startup"))))))

(defn missing-stage-stage
  "This stage is a fallback - if something went wrong and
  a specified stage was requested, but is invalid.
  This is the stage which emits data.
  Boo, you messed up!"
  ([] {:type       :missing-stage
       :hidden     true
       :params {
         :in         [:data-stream "plaeceholder. should never see this"]
        }
       })
  ([stage-def]
    (fn [x]
      (logger/info "Error! This is a missing stage" stage-def))))

(defn dump-s3-stage
  "Collects and then dumps out the data to the sink.
  :size is 10000 by default
  :window is the max time between dumps
  Dumps when it gets to a certain number of records or the time is passed"
  ([] {:description "Periodically dump this set of data records to S3"
       :type       :dump-s3
       :params {
         :in         [:data-stream "The data stream of records to dump to S3"]
         :bucket     [:string "Name of the bucket to put record files into"]
         :secret-key [:string "Your IAM secret key"]
         :access-key [:string "Your IAM access key"]
         :size       [:integer "How many records to buffer before a write. Default is 10000"]
         :window     [:integer "How long to wait between writes (in ms)"]
       }})
  ([stage-def]
    (let [data             (atom [])
          last-dump        (atom (.getTime (java.util.Date.)))
          window           (or (:window stage-def) 5000)
          size             (or (:size stage-def)   10000)
          output-target    (:out stage-def)]
      (fn [x]
        (let [now (.getTime (java.util.Date.))
              wait-until (+ @last-dump window)]
          (swap! data conj x)

          (if (or (> (count @data) size)
                  (> now wait-until))
              (do
                (let [{hive-uuid :hive-uuid
                       worker-uuid :worker-uuid
                       failing-value :data-value} @data]
                  (logger/info "TODO DUMP TO S3" hive-uuid worker-uuid stage-def @data))
                (reset! data [])
                (reset! last-dump (.getTime (java.util.Date.))))))))))

(defn dump-post-stage
  "Collects and then dumps out the data to the sink.
  :size is 10000 by default
  :window is the max time between dumps
  Dumps when it gets to a certain number of records or the time is passed
  Will HTTP POST this to your endpoint"
  ([] {
       :type       :dump-post
       :description "Periodically dump this set of data records to a POST hook"
       :params {
         :in         [:data-stream "The data records to dump to a POST"]
         :url        [:string  "Which URL to POST to"]
         :size       [:integer "How many records to buffer before a write. Default is 10000"]
         :window     [:integer "How long to wait between writes (in ms)"]
       }})
  ([stage-def]
    (let [data             (atom [])
          last-dump        (atom (.getTime (java.util.Date.)))
          window           (or (:window stage-def) 5000)
          size             (or (:size stage-def)   10000)
          output-target    (:out stage-def)]
      (fn [x]
        (let [now (.getTime (java.util.Date.))
              wait-until (+ @last-dump window)]
          (swap! data conj x)

          (if (or (> (count @data) size)
                  (> now wait-until))
              (do
                (let [{hive-uuid :hive-uuid
                       worker-uuid :worker-uuid
                       failing-value :data-value} @data]
                  (logger/info "TODO DUMP TO POST" hive-uuid worker-uuid stage-def @data))
                (reset! data [])
                (reset! last-dump (.getTime (java.util.Date.))))))))))

(defn hive-data-stages-specs
  "Returns all the descriptions of each of the stages that
  can be added to the pipeline"
  []
  (try
    (let [all-stages       [ log-stage
                             dump-s3-stage
                             dump-post-stage
                             missing-stage-stage
                             average-stage
                             alert-email-stage
                             alert-post-stage
                             changed-email-stage
                             changed-post-stage
                            ] ]
      (logger/info "Stages: " all-stages)
      (into {} (map #(let [spec (%)]
                         [(:type spec) {:factory %
                                        :spec spec}])
                    all-stages)))
    (catch Exception e (logger/error "Error: " e))))

(defn hive-data-stages-notify-changes
  "When the hive data stages changed, we emit a :restart to the hive's
  data pipeline."
  [hive-uuid]
  (logger/info "TODO - emit the restart token into the stream" hive-uuid))

(defn hive-data-stages-index
  [hive-uuid]

  (try
    (map
      #(assoc (:params %) :type (:stage_type %))
      (jdbc/query sql-db ["SELECT * FROM hive_data_processing_stages WHERE hive_uuid = ?" (ensure-uuid hive-uuid)]))
    (catch clojure.lang.ExceptionInfo e false)))

(defn hive-data-stages-create
  [hive-uuid stage-type & params-arr]
  (let [params (apply hash-map params-arr)
        stage-type (keyword stage-type)
        spec   (:spec (get (hive-data-stages-specs) stage-type))]
  ;; Validate arguments
  (logger/info "TODO - be more careful about cleaning parameters!")

  ;; Create DB record
  (let [clean-params {:stage_type (str stage-type)
                      :hive_uuid (ensure-uuid hive-uuid)
                      :params params}
        result (first (jdbc/insert! sql-db :hive_data_processing_stages clean-params))
        ]
    (hive-data-stages-notify-changes hive-uuid)
    result
    )))

(defn hive-data-stages-delete
  "Delete a given hive-data-stage from the hive processing!"
  [stage-uuid]
  (let [hive-uuid (:hive_uuid (first (jdbc/query sql-db ["SELECT hive_uuid FROM hive_data_processing_stages WHERE uuid = ?"
                                                  (ensure-uuid stage-uuid)])))
        res (jdbc/delete! sql-db :hive_data_processing_stages ["uuid = ?" (ensure-uuid stage-uuid)])]
    (hive-data-stages-notify-changes hive-uuid)
    res))
