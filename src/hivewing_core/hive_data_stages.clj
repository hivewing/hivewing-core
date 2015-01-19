(ns hivewing-core.hive-data-stages
  (:require
            [taoensso.timbre :as logger]
            [clj-time.core :as ctime]
            ))

(def message-format {
  :hive-uuid   "(always present)"
  :worker-uuid "(may be present / not)"
  :data-name   "(string)"
  :data-value  "(string)"
  :at          "epoch ts or something"
  })

(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (if (re-find #"^-?\d+\.?\d*$" s)
    (read-string s)))

(defn log-step
  ([] {:in         :data-stream
       :type       :log
       :visiblity  :hidden
       :count-rate [:integer "How often you should emit that you have recvd messages"]
       })
  ([step-def]
    (let [step-count (or (:count-rate step-def) 5)
          cnt (atom 0)
          ]
      (fn [x]
        (swap! cnt inc)
        (if (= 0 (mod @cnt step-count))
          (logger/info "processed" @cnt "since startup"))))))

(defn missing-stage-step
  "This stage is a fallback - if something went wrong and
  a specified stage was requested, but is invalid.
  This is the stage which emits data.
  Boo, you messed up!"
  ([] {:in         :data-stream
       :type       :missing-stage
       :visiblity  :hidden
       })
  ([step-def]
    (fn [x]
      (logger/info "Error! This is a missing stage" step-def))))

(defn dump-s3-step
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
  ([step-def]
    (let [data             (atom [])
          last-dump        (atom (.getTime (java.util.Date.)))
          window           (or (:window step-def) 5000)
          size             (or (:size step-def)   10000)
          output-target    (:out step-def)]
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
                  (logger/info "TODO DUMP TO S3" hive-uuid worker-uuid step-def @data))
                (reset! data [])
                (reset! last-dump (.getTime (java.util.Date.))))))))))

(defn dump-post-step
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
  ([step-def]
    (let [data             (atom [])
          last-dump        (atom (.getTime (java.util.Date.)))
          window           (or (:window step-def) 5000)
          size             (or (:size step-def)   10000)
          output-target    (:out step-def)]
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
                  (logger/info "TODO DUMP TO POST" hive-uuid worker-uuid step-def @data))
                (reset! data [])
                (reset! last-dump (.getTime (java.util.Date.))))))))))


(defn push-alert
  "Push an alert - to the system.
  Can usually be an email of POST hook
  "
  [step-def msg-in]
  (let [{hive-uuid :hive-uuid
         worker-uuid :worker-uuid
         failing-value :data-value} msg-in]
    (logger/info "TODO ALERT:" hive-uuid worker-uuid step-def failing-value)))

(defn push-data
  "Push the message out (back into Kinesis really)
  But also through the SQL database, so you can read it
  from the API"
  [msg-in new-data output-target]
  (let [{hive-uuid :hive-uuid
         worker-uuid :worker-uuid} msg-in]
    (logger/info "TODO Sending this out: " hive-uuid worker-uuid output-target new-data)))

(defn averager [step-def]
  "Averages over a window of time.
  Would like to be a better sum of squares algorithm, but I can't find it. Boo.
  Needs :out, :in and :window"

  (let [start-state      {:sum 0 :cnt 0 :avg 0}
        window           (or (:window step-def) 5000)
        output-target    (:out step-def)
        state            (atom start-state)
        last-calculation (atom (.getTime (java.util.Date.)))
        ]
    (fn [x]

      (let [incr-val (or (parse-number (:data-value x)) 0)
            now (.getTime (java.util.Date.))
            wait-until (+ @last-calculation window)
            new-state (swap! state (fn [current]
                          (let [sum (+ incr-val (:sum current))
                                cnt (+ (:cnt current) 1)
                                avg (/ (+ incr-val (* (:avg current) (:cnt current)))
                                       (+ 1 (:cnt current)))]
                            (println "avg now" avg sum cnt)
                              {:sum sum
                               :cnt cnt
                               :avg avg})))
            new-avg (:avg new-state)
          ]
          (if (> now wait-until)
            (do
              (push-data x new-avg output-target)
              (reset! state {:sum new-avg :cnt 1 :avg new-avg})))))))

(defn summer
  "Sums up the sequence of values over time"
  [step-def]
  (let [sum (atom 0)
        last-calculation (atom (.getTime (java.util.Date.)))
        window    (or (:window step-def) 5000)
        output-target (:out step-def)]
    (fn [x]
      (let [incr-val (or (parse-number (:data-value x)) 0)]
        (swap! sum + incr-val)
        (let [now (.getTime (java.util.Date.))
              wait-until (+ @last-calculation window)]
          (if (> now wait-until)
            (do
              (push-data x @sum output-target)
              (reset! sum 0))))))))

(defn alerter [step-def]
  "Alerts when it sees a value tested differently than the baseline.
    :test is [:gt, :gte, :lt, :lte, :eq (default)]
    :value is the value to test against
    The definition goes to push-alert if there was an alert.
    For notification options, look there"
  (let [baseline-value (:value step-def)
        test-func (case (:test step-def)
                    :gt  >
                    :gte >=
                    :lt  <
                    :lte <=
                    "default" =)]
    (fn [x]
      (let [data-value (:data-value x)]
        (if (test-func (or (parse-number data-value) data-value) baseline-value)
          (push-alert step-def x))))))

(defn stages
  "Returns all the descriptions of each of the stages that
  can be added to the pipeline"
  []
  (into {} (map #(let [spec (%)]
                       [(:type spec) {:factory %
                                      :spec spec}])
                       ) [ log-step
                           dump-s3-step
                           dump-post-step
                           missing-stage-step
                          ]))
