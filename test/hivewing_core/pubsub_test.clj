(ns hivewing-core.pubsub-test
  (:require [clojure.test :refer :all]
            [hivewing-core.pubsub :refer :all]))

(deftest do-some-pubsub
  (testing "create a pubsubber and listen"
    (let [recv-count (atom 0)
          listener (subscribe-message "subscribe" (fn [msg]
                                                    (println "recv: " msg)
                                                    (println @recv-count)
                                                    (swap! recv-count inc)
                                                    (println @recv-count)
                                                    (println "OK RECIEVE! ")
                                                  ))]
      (is (= 0 @recv-count))
      (publish-message "subscribe" {"applsauce" "123"})
    ))
  )
