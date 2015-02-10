(ns hivewing-core.public-keys
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.hive-image-notification :as hin]
            [hivewing-core.public-key-notification :as pkn]
            [hivewing-core.hive-manager :as hm]
            [clojure.java.jdbc :as jdbc]))

(defn public-keys-notify-of-bk-change
  [bk-uuid]
    (hin/hive-images-notification-send-beekeeper-update-message bk-uuid)
    ;; Notify all the people listening - the the public keys
    ;; on all the hives that this guy is related to - have changed
    (map #(pkn/public-keys-notify-of-hive-change (:hive_uuid %))
            (hm/hive-managers-managing bk-uuid)))

(defn public-keys-for-beekeeper
  "Finds the public keys of a given beekeeper"
  [bk-uuid]
  (jdbc/query sql-db ["SELECT * FROM public_keys WHERE beekeeper_uuid = ?" (ensure-uuid bk-uuid)]))

(defn public-keys-delete
  "Delete a public key record"
  ([bk-uuid] (doseq [{pk :uuid} (public-keys-for-beekeeper bk-uuid)]
              (public-keys-delete bk-uuid pk)))
  ([bk-uuid public-key-uuid]

    (let [res ((jdbc/delete! sql-db :public_keys ["beekeeper_uuid = ? AND uuid = ?" (ensure-uuid bk-uuid) (ensure-uuid public-key-uuid)]))]
        (public-keys-notify-of-bk-change bk-uuid)
        res)))


(defn public-key-create
  "Creates a new public-key for a beekeeper"
  [bk-uuid public-key-string]

  ; Tell gitolite that things have changed.
  (let [res (first (jdbc/insert! sql-db :public_keys {:beekeeper_uuid (ensure-uuid bk-uuid) :key public-key-string}))]
    (public-keys-notify-of-bk-change bk-uuid)
    res))
