(ns hivewing-core.hive-data-stages-test
  (:require [clojure.test :refer :all]
            [hivewing-core.helpers :refer :all]
            [conjure.core :as conjure]
            [hivewing-core.hive-data-stages :refer :all])
  )

(use-fixtures :each clean-database)

(deftest get-stages
  (is (hive-data-stages-specs)))

(deftest reading-saving-data-processing-stages
  (testing "create the stage"
    (let [{hive-uuid :hive-uuid
           worker-uuid :worker-uuid
           :as create-res} (create-worker)
          stage (hive-data-stages-create hive-uuid
                                         :log
                                         :in {:worker "data"}
                                         :count-rate 1)]
      (= (count (hive-data-stages-index hive-uuid)) 1)
      (hive-data-stages-delete (:uuid stage))
      (= (count (hive-data-stages-index hive-uuid)) 0)
    ))
)

(deftest average-stage-test
  (testing "that worker -> hive works"
    (let [stage ((:factory (get (hive-data-stages-specs) :average)) {:in {:worker "test"}
                                                   :out {:hive "test-avg"}
                                                   :window 0
                                                   })]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-data]
        (doseq [msg (map #(hash-map
                          :hive-uuid "1"
                          :worker-uuid (mod % 3)
                          :data-value (str %)
                          :data-name "test") (range 0 12))]
          (stage msg))
        (Thread/sleep 50 )

        ;; Asset that we're merging all the workers into one
        (let [calls             (last (last @conjure/call-times))]
          (doseq [ call calls]
            (is (= (nth call 0) "1"))
            (is (= (nth call 1) nil))
          ))
      ))

  (testing "that worker -> worker works"
    (let [stage ((:factory (get (hive-data-stages-specs) :average)) {:in {:worker "test"}
                                                   :out {:worker "test-avg"}
                                                   :window 0
                                                   })]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-data]

        (doseq [msg (map #(hash-map
                          :hive-uuid "1"
                          :worker-uuid (mod % 3)
                          :data-value (str %)
                          :data-name "test") (range 0 12))]
          (stage msg))
        (Thread/sleep 1)

        ;; Asset that we're merging all the workers into one
        (let [calls             (last (last @conjure/call-times))]
          (doseq [ [idx call] (map-indexed vector calls)]

            (is (= "1" (nth call 0)))
            (is (= (str (mod idx 3)) (nth call 1)))
            (is (= "test-avg" (nth call 2)))
            (is (= idx (nth call 3)))))
        (conjure/verify-call-times-for push-data 12)
        )
      ))
  (testing "that hive -> hive works"
    (let [stage ((:factory (get (hive-data-stages-specs) :average)) {:in {:hive "test"}
                                                   :out {:hive "test-avg"}
                                                   :window 0
                                                   })]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-data]

        (doseq [msg (map #(hash-map
                          :hive-uuid "1"
                          :worker-uuid nil
                          :data-value (str %)
                          :data-name "test") (range 0 12))]
          (stage msg))
        (Thread/sleep 1)

        ;; Asset that we're merging all the workers into one
        (let [calls             (last (last @conjure/call-times))]
          (doseq [ [idx call] (map-indexed vector calls)]

            (is (= "1" (nth call 0)))
            (is (nil? (nth call 1)))
            (is (= "test-avg" (nth call 2)))
            ))
        (conjure/verify-call-times-for push-data 12)
        )
      ))))

(deftest changed-email-stage-test
  (testing "that you deliver an alert to POST when a worker changes"
    (let [stage ((:factory (get (hive-data-stages-specs) :change-email))
                 { :in  {:worker "test"}
                   :url "http://email-hook.com"})]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-email-alert]
        (let [msg (hash-map
                          :hive-uuid "1"
                          :worker-uuid "1"
                          :data-value nil
                          :data-name "test")]

          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-email-alert 1)
          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-email-alert 1)
          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-email-alert 1)
          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-email-alert 1)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-email-alert 2)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-email-alert 2)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-email-alert 2)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-email-alert 2)
          (stage (assoc msg :data-value "2"))
          (conjure/verify-call-times-for push-email-alert 3)
          (stage (assoc msg :data-value "3"))
          (conjure/verify-call-times-for push-email-alert 4)
        )))
    )
)
(deftest changed-post-stage-test
  (testing "that you deliver an alert to POST when a worker changes"
    (let [stage ((:factory (get (hive-data-stages-specs) :change-post))
                 { :in  {:worker "test"}
                   :url "http://post-hook.com"})]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-post-alert]
        (let [msg (hash-map
                          :hive-uuid "1"
                          :worker-uuid "1"
                          :data-value nil
                          :data-name "test")]

          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-post-alert 1)
          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-post-alert 1)
          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-post-alert 1)
          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-post-alert 1)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-post-alert 2)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-post-alert 2)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-post-alert 2)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-post-alert 2)
          (stage (assoc msg :data-value "2"))
          (conjure/verify-call-times-for push-post-alert 3)
          (stage (assoc msg :data-value "3"))
          (conjure/verify-call-times-for push-post-alert 4)
        )))
    )
)
(deftest alert-post-stage-test
  (testing "that you deliver an alert to POST when a worker goes over"
    (let [stage ((:factory (get (hive-data-stages-specs) :alert-post))
                 {
                    :in  {:worker "test"}
                    :url "http://post-hook.com"
                    :value "2"
                    :test :gt
                    :window 0
                 })]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-post-alert]
        (let [msg (hash-map
                          :hive-uuid "1"
                          :worker-uuid "1"
                          :data-value nil
                          :data-name "test")]

          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-post-alert 0)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-post-alert 0)
          (stage (assoc msg :data-value "2"))
          (conjure/verify-call-times-for push-post-alert 0)
          (stage (assoc msg :data-value "3"))
          (conjure/verify-call-times-for push-post-alert 1)
        )))
    )
    (testing "that you deliver an alert to POST when a worker goes gte"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-post))
                   {
                      :in  {:worker "test"}
                      :url "http://post-hook.com"
                      :value "2"
                      :test :gte
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-post-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-post-alert 0)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-post-alert 0)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-post-alert 1)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-post-alert 2)
          ))))

    (testing "that you deliver an alert to POST when a worker goes lt"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-post))
                   {
                      :in  {:worker "test"}
                      :url "http://post-hook.com"
                      :value "2"
                      :test :lt
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-post-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-post-alert 1)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-post-alert 2)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-post-alert 2)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-post-alert 2)
          ))))
    (testing "that you deliver an alert to POST when a worker goes lte"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-post))
                   {
                      :in  {:worker "test"}
                      :url "http://post-hook.com"
                      :value "2"
                      :test :lte
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-post-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-post-alert 1)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-post-alert 2)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-post-alert 3)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-post-alert 3)
          ))))
    (testing "that you deliver an alert to POST when a worker goes eq"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-post))
                   {
                      :in  {:worker "test"}
                      :url "http://post-hook.com"
                      :value "2"
                      :test :eq
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-post-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-post-alert 0)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-post-alert 0)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-post-alert 1)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-post-alert 1)
          ))))
)

(deftest alert-email-stage-test
  (testing "that you deliver an alert to email when a worker goes over"
    (let [stage ((:factory (get (hive-data-stages-specs) :alert-email))
                 {
                    :in  {:worker "test"}
                    :value "2"
                    :test :gt
                    :window 0
                 })]
      ;; We want to push in some worker data.
      ;; Since it is going from both workers to the output
      (conjure/mocking [push-email-alert]
        (let [msg (hash-map
                          :hive-uuid "1"
                          :worker-uuid "1"
                          :data-value nil
                          :data-name "test")]

          (stage (assoc msg :data-value "0"))
          (conjure/verify-call-times-for push-email-alert 0)
          (stage (assoc msg :data-value "1"))
          (conjure/verify-call-times-for push-email-alert 0)
          (stage (assoc msg :data-value "2"))
          (conjure/verify-call-times-for push-email-alert 0)
          (stage (assoc msg :data-value "3"))
          (conjure/verify-call-times-for push-email-alert 1)
        )))
    )
    (testing "that you deliver an alert to email when a worker goes gte"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-email))
                   {
                      :in  {:worker "test"}
                      :url "http://email-hook.com"
                      :value "2"
                      :test :gte
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-email-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-email-alert 0)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-email-alert 0)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-email-alert 1)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-email-alert 2)
          ))))
    (testing "that you deliver an alert to email when a worker goes lt"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-email))
                   {
                      :in  {:worker "test"}
                      :url "http://email-hook.com"
                      :value "2"
                      :test :lt
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-email-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-email-alert 1)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-email-alert 2)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-email-alert 2)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-email-alert 2)
          ))))
    (testing "that you deliver an alert to email when a worker goes lte"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-email))
                   {
                      :in  {:worker "test"}
                      :url "http://email-hook.com"
                      :value "2"
                      :test :lte
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-email-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-email-alert 1)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-email-alert 2)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-email-alert 3)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-email-alert 3)
          ))))
    (testing "that you deliver an alert to email when a worker goes eq"
      (let [stage ((:factory (get (hive-data-stages-specs) :alert-email))
                   {
                      :in  {:worker "test"}
                      :url "http://email-hook.com"
                      :value "2"
                      :test :eq
                      :window 0
                   })]
        ;; We want to push in some worker data.
        ;; Since it is going from both workers to the output
        (conjure/mocking [push-email-alert]
          (let [msg (hash-map
                            :hive-uuid "1"
                            :worker-uuid "1"
                            :data-value nil
                            :data-name "test")]

            (stage (assoc msg :data-value "0"))
            (conjure/verify-call-times-for push-email-alert 0)
            (stage (assoc msg :data-value "1"))
            (conjure/verify-call-times-for push-email-alert 0)
            (stage (assoc msg :data-value "2"))
            (conjure/verify-call-times-for push-email-alert 1)
            (stage (assoc msg :data-value "3"))
            (conjure/verify-call-times-for push-email-alert 1)
          ))))
)
