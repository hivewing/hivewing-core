(ns hivewing-core.hive-data-processing
  (:require
            [taoensso.timbre :as logger]
            [hivewing-core.configuration :as config]
            [amazonica.aws.sqs :as sqs]
            [hivewing-core.hive-data :as hd]
            [hivewing-core.hive-data-stages :as hds]
            [clojure.core.async :as async]))

(comment
  (create-msg-filter (first (load-pipeline-description "1")))
  )

(defn create-msg-filter [ stage-def ]
  (let [worker-selector (first (keys (:in stage-def)))
        data-name       (first (vals (:in stage-def)))]
    (fn [x]
      (and (or
             ;; If it's ANY worker, it should
             ;;   allow it through if it has a worker
             (and (= :worker worker-selector)
                  (not (nil? (:worker-uuid x))))
             ;; If it's a hive-data, it needs a nil worker-uuid
             (and (= :hive worker-selector) (nil? (:worker-uuid x))))
           (= (:data-name x) data-name)))))

(defn create-pipeline-stage
  "Create a pipeline stage based upon the passed in stage-definition"
  [ stage-def ]
  (let [msg-filter (create-msg-filter stage-def)
        stages     (hds/stages)
        selected-stage (get stages (:type stage-def))
        safe-stage (or selected-stage (get stages :missing-stage))
        transform  ((:factory safe-stage) stage-def)
        ]
    (fn [x]
      (if (msg-filter x)
        (do
          (transform x))))))

(defn create-processing-pipeline
  "A pipeline is a single transducer
  Which does fanning / filtering / etc. as described by the description
  "
  [pipeline-description]
  (let [pipeline-stages (map create-pipeline-stage pipeline-description)]
    (fn [step]
      (fn
        ([r] (step r))
        ([r x]
         (doseq [pipeline-stage pipeline-stages]
            (try
              (pipeline-stage x)
              (catch Exception e (logger/error "Error: " e))))
         (step r x)
         )))))

(defn drink-the-firehose!
  "Processing function returns a single channel
  data is a channel containing lots of messages , native clojure hash-maps

  If a message for a \"hive-uuid\" has a key :restart = true
  Then you should re-create that handler processor

  When the channel closes, we have been asked to shut-down"
  []
  (let [channel (async/chan 1000)
        incoming-queue (hd/hive-data-sqs-queue)
        results (future (loop []
                          (try
                            (let [msgs (:messages (sqs/receive-message
                                                    config/sqs-aws-credentials
                                                   :queue-url incoming-queue
                                                   :wait-time-seconds 1
                                                   :max-number-of-messages 10
                                                   :delete true))]
                              (if (empty? msgs)
                                (do
                                  (logger/info "No messages to retrieve" incoming-queue)
                                  (Thread/sleep 5000))
                                (doseq [packed-msg msgs]
                                  ; Unpack it - it's just prn-str for now.
                                  (let [msg (read-string (:body packed-msg))]
                                    (logger/info "Putting msg onto channel" channel msg)
                                    ; Process
                                    (async/put! channel msg)
                                    ))))
                            (catch Exception e (logger/error "Error" e)))
                          (recur)))
        ]
    channel))
