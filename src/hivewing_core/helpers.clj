(ns hivewing-core.helpers
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.carmine :as car ]
            [hivewing-core.hive-manager :refer :all]
            [hivewing-core.configuration :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.hive-image :refer :all]
            [hivewing-core.apiary :refer :all]
            [hivewing-core.worker :refer :all]
            [hivewing-core.worker-config :refer :all]
            [hivewing-core.pubsub :refer :all]
            [hivewing-core.beekeeper :refer :all]
            [clj-jgit.porcelain :as cljgit]
            [clojure.java.io :as io]
            )
 (:import [org.eclipse.jgit.api Git InitCommand ]))

(defn clean-database [test-function]
  ; Clear the hive-images
  (ensure-hive-image-bucket :delete_first)
  (redis (car/flushall))

  (jdbc/execute! sql-db ["TRUNCATE TABLE beekeepers, apiaries, hive_managers, hives, workers, public_keys, hivelogs, worker_configs, hivedata"])

  (test-function)
  )

(defn create-hive
  []
  (let [
          beekeeper-uuid (:uuid (beekeeper-create {:email "my_hive@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-uuid      (:uuid (hive-create {:apiary_uuid apiary-uuid}))
          hive-manager-uuid (:uuid   (hive-manager-create hive-uuid beekeeper-uuid))
        ]

      (hash-map  :beekeeper-uuid beekeeper-uuid
                 :apiary-uuid apiary-uuid
                 :hive-uuid hive-uuid
                 :hive-manager-uuid hive-manager-uuid)))

(defn create-worker
  []
  (let [
          beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          apiary-uuid    (:uuid (apiary-create {:beekeeper_uuid beekeeper-uuid}))
          hive-uuid      (:uuid (hive-create {:apiary_uuid apiary-uuid}))
          hive-manager-uuid (:uuid   (hive-manager-create hive-uuid beekeeper-uuid))
          worker-result  (worker-create {:apiary_uuid apiary-uuid :hive_uuid hive-uuid})
          worker-retrieval  (worker-get (:uuid worker-result))
          ]
      (hash-map  :worker-uuid (:uuid worker-retrieval)
                 :beekeeper-uuid beekeeper-uuid
                 :apiary-uuid apiary-uuid
                 :hive-uuid hive-uuid
                 :hive-manager-uuid hive-manager-uuid)))

(defn mk-tmp-dir!
  "Creates a unique temporary directory on the filesystem. Typically in /tmp on
  *NIX systems. Returns a File object pointing to the new directory. Raises an
  exception if the directory couldn't be created after 10000 tries."
  []
  (let [base-dir (io/file (System/getProperty "java.io.tmpdir"))
        base-name (str (System/currentTimeMillis) "-" (long (rand 1000000000)) "-")
        tmp-base (str base-dir "/" base-name)
        max-attempts 10000]
    (loop [num-attempts 1]
      (if (= num-attempts max-attempts)
        (throw (Exception. (str "Failed to create temporary directory after " max-attempts " attempts.")))
        (let [tmp-dir-name (str tmp-base num-attempts)
              tmp-dir (io/file tmp-dir-name)]
          (if (.mkdir tmp-dir)
            tmp-dir
            (recur (inc num-attempts))))))))

(defn create-temp-hive-image-repo
  "Creates a temporary git repository.
  Returns the fake hive uuid, and the temp dir you should
  overwrite gitolite-repositories-root with
  YOU are responsible for deleting it!"
  []
  (let [
        root-dir (mk-tmp-dir!)
        uuid (str (java.util.UUID/randomUUID))
        git-dir (str root-dir "/working-" uuid ".git")
        bare-git-dir (str root-dir "/" uuid ".git")
        repo (cljgit/git-init git-dir)
        ]

    (do
      (spit (str git-dir "/Queenfile") "#Queenfile\n")
      (cljgit/git-add repo "Queenfile")
      (cljgit/git-commit repo "Added queenfile")
      (cljgit/git-clone (str "file://" git-dir) bare-git-dir "origin" "master" true))
    (hash-map :root-dir root-dir :hive-uuid uuid :repo repo)))
