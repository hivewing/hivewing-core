(ns hivewing-core.hive-image
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [digest :as digest]
            [clojure.tools.file-utils :as file-utils]
            [hivewing-core.hive-manager :as hive-manager]
            [environ.core  :refer [env]]))

; sends ref to workers. the imge is a URL.
; http://gitolite.com/gitolite/odds-and-ends.html#server-side-admin

(defn hive-image-recompile-gitolite
  "Run the commands to recompile gitolite"
  [])

(defmacro with-gitolite
  "Recompiles gitolite once you make all these changes."
  [& body] `( ~@body (hive-image-recompile-gitolite)))

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

(defn hive-image-gitolite-user-name
  [user-uuid]
  (str "user-" user-uuid))

(defn hive-image-access-config-file
  "Create and return the string which represents
   a gitolite config file for a hive"
  [hive-uuid]
    (let [manager-uuids (map hive-image-gitolite-user-name (map :beekeeper_uuid (hive-manager/hive-managers-get hive-uuid)))
          user-group-name (str "@hive-" hive-uuid "-users")]
      ; We want to get all the hive-managers
      ; and their uuids added.
      (clojure.string/join
        "\n"
        [
          "# This is a config file describing the access for the hive's images"
          ""
          (map #(str  user-group-name " = " %1) manager-uuids)
          ""
          (str "repo " hive-uuid)
          (str "  C   = " user-group-name)
          (str "  RW+ = " user-group-name)
        ]
        )
    ))

(defn hive-image-user-public-key-name
  "Gives a unique dir for a public-key"
  [user-uuid pk]
  (str (digest/md5 pk) "/" (hive-image-gitolite-user-name user-uuid)))

(defn hive-image-set-user-public-keys
  "Given a worker-uuid and their public keys
  it will make sure that these are the only ones that
  are available in the gitolite system
  Public_keys are unique across the system by DB design"
  [user-uuid & public-keys]
    (let [user-keys-path (.getPath (io/file gitolite-key-root (uuid-split user-uuid)))]

      ; Delete the existing keys
      (file-utils/recursive-delete (io/file user-keys-path))

      ; Generate the new keys
      (doseq [public-key public-keys]
        (let [location (io/file user-keys-path (hive-image-user-public-key-name user-uuid public-key))]
          (doall
            (.mkdirs (.getParent location))
            ; add them to the file system
            (spit location public-key))))))

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
