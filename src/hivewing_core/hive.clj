(ns hivewing-core.hive
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.hive-image-notification :as hin]
            [hivewing-core.namer :as namer]
            [hivewing-core.worker :refer [worker-list]]
            [hivewing-core.worker-config :refer [worker-config-set-hive-image]]
            [clojure.java.jdbc :as jdbc]))


(defn hive-can-modify?
  [bk-uuid hive-uuid]
  (if (and hive-uuid bk-uuid)
    (jdbc/query sql-db ["SELECT beekeeper_uuid FROM hive_managers WHERE hive_uuid = ? AND beekeeper_uuid = ? LIMIT 1" (ensure-uuid hive-uuid) (ensure-uuid bk-uuid)] :result-set-fn first)
    nil
    ))

(defn hive-get-permissions
  "Gets access roles for a given uuid and token"
  [hive-uuid token]
  (if (and hive-uuid token)
    (jdbc/query sql-db ["SELECT can_write, beekeeper_uuid FROM hive_managers WHERE hive_uuid = ? AND hive_managers.uuid = ? LIMIT 1" (ensure-uuid hive-uuid) (ensure-uuid token)] :result-set-fn first)
    nil
    ))

(defn hive-delete
  "Deletes the hive record.  You need to have deleted all
  the hive managers first though!"
  [hive-uuid]
  (hin/hive-images-notification-send-hive-update-message hive-uuid)
  (jdbc/delete! sql-db :hives ["uuid = ?" (ensure-uuid hive-uuid)]))

(defn hive-get
  "Gets the data for a given hive"
  [hive-uuid]
  (jdbc/query sql-db ["SELECT * FROM hives WHERE uuid = ? LIMIT 1" (ensure-uuid hive-uuid)] :result-set-fn first))

(defn hive-create
  "Creates a new hive"
  [{hive-name :name
    apiary-uuid :apiary_uuid :as parameters}]

  (let [clean-params (assoc parameters
                            :apiary_uuid (ensure-uuid apiary-uuid)
                            :name (or hive-name (str "home of " (namer/gen-name))))
        result (first (jdbc/insert! sql-db :hives parameters))
        {hive-uuid :uuid} result ]

    (hin/hive-images-notification-send-hive-update-message hive-uuid)
    result))

(defn hive-update-hive-image-url
  "Set this value on every worker in the given hive.
  Will publish a change message for any worker which
  did not have that config set already"
  ([hive-uuid hive-image-url]
    (hive-update-hive-image-url hive-uuid hive-image-url 1 100))

  ([hive-uuid hive-image-url page per-page]
    (let [worker-uuids (worker-list hive-uuid :per-page per-page :page page)]
      (if (not (empty? worker-uuids))
        (pmap
          #(worker-config-set-hive-image %1 hive-image-url hive-uuid)
          worker-uuids
          )
        (recur hive-uuid hive-image-url (+ 1 page) per-page)
        ))))
