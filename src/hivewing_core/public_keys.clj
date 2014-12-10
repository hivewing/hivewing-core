(ns hivewing-core.public-keys
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.java.jdbc :as jdbc]))

(defn public-keys-for-beekeeper
  "Finds the public keys of a given beekeeper"
  [bk-uuid]
  (jdbc/query sql-db ["SELECT * FROM public_keys WHERE beekeeper_uuid = ?" (ensure-uuid bk-uuid)]))

(defn public-keys-delete
  "Delete a public key record"
  ([bk-uuid] (doseq [{pk :uuid} (public-keys-for-beekeeper bk-uuid)]
              (public-keys-delete bk-uuid pk)))
  ([bk-uuid public-key-uuid]
    (jdbc/delete! sql-db :public_keys ["beekeeper_uuid = ? AND uuid = ?" (ensure-uuid bk-uuid) (ensure-uuid public-key-uuid)])))

(defn public-key-create
  "Creates a new public-key for a beekeeper"
  [bk-uuid public-key-data]
  (first (jdbc/insert! sql-db :public_keys {:beekeeper_uuid (ensure-uuid bk-uuid) :key public-key-data})))
