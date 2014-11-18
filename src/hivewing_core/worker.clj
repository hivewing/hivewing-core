(ns hivewing-core.worker
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn worker-list
  "Gets the list of worker uuids. It is paginated
   and per_page is limited.  Returns the total as well.
   Does NOT return the access_token"
  [hive-uuid & params]
  (let [params   (into {:page 1, :per-page 50} (apply hash-map  params))
        per-page (if (> (:per-page params) 100) 100 (:per-page params))
        page     (:page params)
        ]
    (jdbc/query sql-db ["SELECT name, created_at, updated_at, apiary_uuid, hive_uuid FROM workers WHERE hive_uuid = ? LIMIT ? OFFSET ?"
                        (ensure-uuid hive-uuid)
                        per-page
                        (* (- page 1) per-page)])))

(defn worker-get
  "Get the data for worker record.  You pass in the worker via the worker uuid.  Returns the data
  as a hashmap. DOES NOT return the access_token"
  [worker-uuid]
  (jdbc/query sql-db ["SELECT name, created_at, updated_at, apiary_uuid, hive_uuid FROM workers WHERE uuid = ? LIMIT 1" (ensure-uuid worker-uuid)] :result-set-fn first))

(defn worker-create
  [{apiary-uuid :apiary_uuid hive-uuid :hive_uuid :as parameters}]

  (let [clean-params (assoc parameters
                            :apiary_uuid (ensure-uuid apiary-uuid)
                            :hive_uuid (ensure-uuid hive-uuid))]
    (first (jdbc/insert! sql-db :workers clean-params))))

(defn worker-join-hive
  "Add a worker to a given hive. Removing it from a hive it was in before.
  That hive MUST be part of the same apiary.  If not, you should use
  worker-join-apiary first (which sets the hive to nil)"
  [worker-uuid hive-uuid]
  (println "TODO"))

(defn worker-join-apiary
  "Adds the worker to an apiary, setting the hive_uuid to nil"
  [worker-uuid apiary-uuid]
  (println "TODO"))

(defn worker-access?
  "Worker given the token and uuid, can it access?"
  [worker-uuid access-token]
  (:uuid (jdbc/query sql-db ["SELECT uuid FROM workers WHERE uuid = ? AND access_token = ? LIMIT 1"
                      (ensure-uuid worker-uuid)
                      (ensure-uuid access-token)] :result-set-fn first)))

;(worker-access? "5130d89a-6f5b-11e4-8dbf-8332fcc50bc0" "a1182454-e1f1-4c7d-b94c-c92e732d441f")
(defn worker-reset-access-token
  [worker-uuid]
  (jdbc/execute! sql-db ["UPDATE workers SET access_token = uuid_generate_v4() WHERE uuid = ?" (ensure-uuid worker-uuid)])
  (worker-get (ensure-uuid worker-uuid)))
