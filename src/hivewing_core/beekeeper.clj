(ns hivewing-core.beekeeper
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.public-keys :refer :all]
            [hivewing-core.hive-manager :refer :all]
            [hivewing-core.worker-config :as worker-config]
            [clojure.java.jdbc :as jdbc]))

(defn beekeeper-get
  "Get the information for a given beekeeper"
  [beekeeper-uuid]
  (jdbc/query sql-db ["SELECT * FROM beekeepers WHERE uuid = ? LIMIT 1" (ensure-uuid beekeeper-uuid)] :result-set-fn first))

(defn beekeeper-find-by-email
  "Find the beekeeper by their email address"
  [email]
  (jdbc/query sql-db ["SELECT * FROM beekeepers WHERE LOWER(email) = LOWER(?)", email] :result-set-fn first))

(defn beekeeper-create
  "Create a new beekeeper"
  [parameters]
  ; it returns a vector, we want the first value.
  (first (jdbc/insert! sql-db :beekeepers parameters)))

(defn beekeeper-delete
  "Delete a beekeeper.
  We leave their hive and workers alone for now.
  Those should be deleted in a different way with the UI or something
  so deleting a user doesn't bring down a whole cloud of stuff
  And then it's as simple as deleting auser and you can crash a whole
  application / system"
  [beekeeper-uuid]
  (let [bk-uuid (ensure-uuid beekeeper-uuid)]
    (do
      ; Delete their public keys
      (public-keys-delete bk-uuid)
      ; Delete the user!
      (jdbc/delete! :beekeepers ["uuid = ?" bk-uuid]))))
