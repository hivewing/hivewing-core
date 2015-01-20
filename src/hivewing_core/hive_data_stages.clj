(ns hivewing-core.hive-data-stages
  (:require
            [taoensso.timbre :as logger]
            [clj-time.core :as ctime]
            ))

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
  ([] {:in         :data-stream
       :type       :alert-email
       :email      [:email   "The email that the hook will hit"]
       :value      [:string "The value we are testing against"]
       :test       [[:gt :gte :lt :lte :eq] "The comparator"]
       })
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
  ([] {:in         :data-stream
       :type       :alert-post
       :url        [:url   "The URL that the POST hook will hit"]
       :value      [:string "The value we are testing against"]
       :test       [[:gt :gte :lt :lte :eq] "The comparator"]
       })
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

(defn average-stage
  ([] {:in         :data-stream
       :type       :average
       :out        [:data-stream "Name of the output field the average should go to"]
       :window     [:integer "How long to wait between averages (in ms)"]
       })
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
  ([] {:in         :data-stream
       :type       :log
       :visiblity  :hidden
       :count-rate [:integer "How often you should emit that you have recvd messages"]
       })
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
  ([] {:in         :data-stream
       :type       :missing-stage
       :visiblity  :hidden
       })
  ([stage-def]
    (fn [x]
      (logger/info "Error! This is a missing stage" stage-def))))

(defn dump-s3-stage
  "Collects and then dumps out the data to the sink.
  :size is 10000 by default
  :window is the max time between dumps
  Dumps when it gets to a certain number of records or the time is passed"
  ([] {:in         :data-stream
       :type       :dump-s3
       :bucket     [:string "Name of the bucket to put record files into"]
       :secret-key [:string "Your IAM secret key"]
       :access-key [:string "Your IAM access key"]
       :size       [:integer "How many records to buffer before a write. Default is 10000"]
       :window     [:integer "How long to wait between writes (in ms)"]
       })
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
  ([] {:in         :data-stream
       :type       :dump-post
       :url        [:string  "Which URL to POST to"]
       :size       [:integer "How many records to buffer before a write. Default is 10000"]
       :window     [:integer "How long to wait between writes (in ms)"]
       })
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

(defn stages
  "Returns all the descriptions of each of the stages that
  can be added to the pipeline"
  []
  (into {} (map #(let [spec (%)]
                       [(:type spec) {:factory %
                                      :spec spec}])
                       ) [ log-stage
                           dump-s3-stage
                           dump-post-stage
                           missing-stage-stage
                           average-stage
                           alert-email-stage
                           alert-post-stage
                          ]))
