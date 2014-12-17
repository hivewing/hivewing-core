(ns hivewing-core.hive-images-test
  (:require [clojure.test :refer :all]
            [conjure.core :as conjure]
            [hivewing-core.helpers :refer :all]
            [hivewing-core.configuration :as config]
            [hivewing-core.beekeeper :refer :all]
            [hivewing-core.hive-image :refer :all]
            [hivewing-core.hive-image-notification :refer :all]
            [hivewing-core.hive-manager :refer :all]
            [hivewing-core.hive :refer :all]
            [hivewing-core.apiary :refer :all]
            [clojure.java.jdbc :refer :all]
            [clojure.tools.file-utils :as file-utils]
            [clojure.java.io :as io]
            [hivewing-core.public-keys :refer :all]))

(use-fixtures :each clean-database)

(deftest hive-images
  (testing "create the config file"
    (let [
          beekeeper-uuid (:uuid (beekeeper-create {:email "my_email@example.com"}))
          public-key     (public-key-create beekeeper-uuid "public-key")
          apiary         (apiary-create {:beekeeper_uuid beekeeper-uuid})
          hive           (hive-create {:apiary_uuid (:uuid apiary)})
          hive-manager   (hive-manager-create (:uuid hive) beekeeper-uuid)
          ]
        (is (hive-image-access-config-file (:uuid hive)))
      )
    (testing "create the queue"
      (is (hive-images-notification-sqs-queue)))))

(deftest hive-image-packaging
  (let [{root-dir :root-dir hive-uuid :hive-uuid} (create-temp-hive-image-repo)]
    (with-redefs [gitolite-repositories-root (.getPath root-dir)]
      (let [reference (hive-image-resolve-ref hive-uuid "master")
            url (hive-image-package-image hive-uuid reference)]
        (is url)
      (file-utils/recursive-delete (io/file root-dir))))))
