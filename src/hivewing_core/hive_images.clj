
(ns hivewing-core.hive-images
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [hivewing-core.hive-manager :as hive-manager]
            [environ.core  :refer [env]]))

; sends ref to workers. the imge is a URL.
; http://gitolite.com/gitolite/odds-and-ends.html#server-side-admin

(defn hive-images-recompile-gitolite
  "Run the commands to recompile gitolite"
  [])

(defmacro with-gitolite
  "Recompiles gitolite once you make all these changes."
  [& body] `( ~@body (hive-images-recompile-gitolite)))

(def gitolite-root
  (.getPath (io/file (env :hivewing-gitolite-root))))

(def gitolite-key-root
  "The place where we'll drop public key files"
  (.getPath (io/file gitolite-root "keydir")))

(def gitolite-conf-root
  "The place where we'll drop repo configuration / hive configuration files"
  (.getPath (io/file gitolite-root "conf" "hives")))

(defn- uuid-split
  "Splits a uuid into a 14 pair path."
  [uuid]
  (partition 2 (clojure.string/replace uuid #"-" "")))

(defn hive-images-access-config-file
  "Create and return the string which represents
   a gitolite config file for a hive"
  [hive-uuid]
    (let [manager-uuids (map :beekeeper_uuid (hive-manager/hive-managers-get hive-uuid))
          user-group-name (str "@hive-" hive-uuid "-users")]
    ; We want to get all the hive-managers
    ; and their uuids added.
    (clojure.string/join
      [
        "# This is a config file describing the access for the hive's images"
        ""
        (map #(str  user-group-name " = " %1) manager-uuids)
        ""
        (str "repo " hive-uuid "/[a-zA-Z0-9].*")
        (str "  C   = " user-group-name)
        (str "  RW+ = " user-group-name)
      ]
      "\n")
    ))

(defn hive-images-set-user-public-keys
  "Update a user's public keys and add them to the
  gitolite system.  You can add multiple public-keys
  It will set them, removing all and then adding them all"
  [worker-uuid & public-keys]
    ; we want to add these public-keys
  )

; Make this an SQS worker to do the updates!
;  Given a hive-uuid - it updates.
;   if it can't find a hive - it deletes the file.

;  Given a user uuid - it updates (pubkeys).
;   if it can't find the user - it deletes all the user's keys
    ; it should delete the hive first! BTW.

; .the-root dir should have
; ~/.gitolite/keydir
  ;  Add "users" with <uu-id-split>/<db_id>/user-<uuid>.pub

; ~/.gitolite/conf
  ;  Create "hives" with: .gitolite/conf/hives/<uu-id-split>/<uuid>.conf

  ;  CONTENTS
  ;   @hive-<hive-uuid>-users = user-<uuid>
  ;   @hive-<hive-uuid>-users = user-<uuid>
  ;   @hive-<hive-uuid>-users = user-<uuid>
  ;   @hive-<hive-uuid>-users = user-<uuid>
  ;   @hive-<hive-uuid>-users = user-<uuid>

  ; repo 12345678-1234-1234-1234-12345678/[a-zA-Z0-9].*
  ;      C = @hive-uuid-users
  ;      RW+ = @hive-uuid-users

; COOKBOOK
;    https://github.com/sitaramc/gitolite-doc/blob/master/cookbook.mkd

; when changes are made --
;  gitolite compile; gitolite trigger POST_COMPILE
;(gitolite -- add user to keys (add to gitolite-admin.git / keydir)
 ; file names is (some descriptive name for the key)<user-uuid>.pub

;load-repo "path-to-git-repo-folder"
