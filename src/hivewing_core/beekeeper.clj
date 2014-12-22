(ns hivewing-core.beekeeper
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.public-keys :refer :all]
            [hivewing-core.hive-manager :refer :all]
            [hivewing-core.hive-image-notification :as hin]
            [hivewing-core.worker-config :as worker-config]
            [clojure.java.jdbc :as jdbc])
    (:import (java.security MessageDigest)
             (org.mindrot.jbcrypt BCrypt)))

(defn- gensalt
    "Generate a salt of an optional size."
      ([] (BCrypt/gensalt))
        ([size] (BCrypt/gensalt size)))

(defn- encrypt
    "Encrypt a plain text string with an auto-generated or supplied salt."
      ([plain-text] (encrypt (gensalt) plain-text))
        ([salt plain-text] (BCrypt/hashpw plain-text salt)))

(defn- check-password
    "Check a plain text string against an encrypted string."
      [plain-text encrypted] (BCrypt/checkpw plain-text encrypted))

(defn beekeeper-public-fields
  [ & opts]
  (if (some #(= :include-all-fields %) (flatten opts))
    "*"
    (clojure.string/join "," ["uuid" "email"])))

(defn beekeeper-get
  "Get the information for a given beekeeper"
  [beekeeper-uuid & opts]
  (jdbc/query sql-db [(str "SELECT "
                           (beekeeper-public-fields opts)
                           "  FROM beekeepers WHERE uuid = ? LIMIT 1")
                      (ensure-uuid beekeeper-uuid)] :result-set-fn first))

(defn beekeeper-find-by-email
  "Find the beekeeper by their email address"
  [email & opts]
  (jdbc/query sql-db [(str "SELECT "
                           (beekeeper-public-fields opts)
                          " FROM beekeepers WHERE LOWER(email) = LOWER(?)")
                      email] :result-set-fn first))

(defn beekeeper-validate
  "Validate that a beekeeper can be logged in, given the email and password"
  [email password]
  (let [bk (beekeeper-find-by-email email :include-all-fields)]
    (if bk
      (let [stored-pw   (:encrypted_password bk)
            valid (check-password password stored-pw)
            ]
        (if valid
          bk
          nil))
      nil)))

(defn beekeeper-create
  "Create a new beekeeper"
  [parameters]
  ; it returns a vector, we want the first value.

  (let [clean-params (select-keys parameters [:email])
        res (first (jdbc/insert! sql-db :beekeepers parameters))]
    (hin/hive-images-notification-send-beekeeper-update-message (:uuid res))
    res))

(def beekeeper-password-regex
  #"^(?=.{6,32}$)(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9$!_-]).*")

(defn beekeeper-set-password
  "Set the password on a beekeeper, without a password they can't log in."
  [bk-uuid password]

  (if (re-find beekeeper-password-regex password)
    (let [enc-password (encrypt password)]
        (jdbc/update! sql-db
                      :beekeepers
                      {:encrypted_password enc-password}
                      ["uuid = ?" (ensure-uuid bk-uuid)])
      )
    (throw (Exception. "Password was not complicated enough!"))
  ))

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
      ; Notify the gitolite that this user is deleted!
      (hin/hive-images-notification-send-beekeeper-update-message bk-uuid)
      ; Delete the user!
      (jdbc/delete! sql-db :beekeepers ["uuid = ?" bk-uuid]))))
