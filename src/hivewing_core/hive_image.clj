(ns hivewing-core.hive-image
  (:require [clojure.java.shell :as shell]
            [taoensso.timbre :as logger]
            [clojure.java.io :as io]
            [biscuit.core :as crc]
            [amazonica.aws.sqs :as sqs]
            [amazonica.aws.s3 :as s3]
            [amazonica.aws.s3transfer :as s3-transfer]
            [digest :as digest]
            [clojure.tools.file-utils :as file-utils]
            [clj-jgit.porcelain :as jgit]
            [clj-jgit.querying  :as jgit-q]
            [clj-jgit.internal  :as jgit-i]
            [hivewing-core.configuration :as config]
            [hivewing-core.crypto :as crypto]
            [hivewing-core.hive-manager :as hive-manager]
            [clojure.data.json :as json]
            [environ.core  :refer [env]])

  (:import
    [java.io File FileOutputStream IOException StringWriter]
    [java.util.zip ZipEntry ZipFile ZipOutputStream]))

(comment
  (def bk-uuid "dfc2ce62-7972-11e4-abcd-a7a75eb23080")
  (public-key-create "dfc2ce62-7972-11e4-abcd-a7a75eb23080"
    "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCf/VDku3r+GRYCoEk95S5jKFJDhFqp7TRbugCl1Y3cFnCtcVG1qR9OxpbOIsOafI5B2fNhVFB7Xq1t8Hgw1poeZj7NsePSiTLquwr+0p4ISGl/yfYqDBfDdI/VSVdz73iU+L5gwa+d4ERLkKYDWS6isuw7eHf6zJ1YAjOFiDto4Fnh7QDWdwJPrQJD5afAQgEeT8Z1VO8YjXHWLOYcGCObMk5dRKYmRDTnA+ZsMm+0WuYZkgtSjyoUcpDsgbzJKYC86cVG+e2Upw8aw+NSYqFBw4VtZgTTqx1U5iZZ+Qe5FvyqJLQn6hZPC9hV7AfGacPZoL2nUl+BkulNXXEk25kv cfitzhugh@cfitz-server")
  (hive-image-set-beekeeper-public-keys bk-uuid public-keys)
  (def hive-uuid "dfc5d2c4-7972-11e4-8732-b334ee7e2863")
  (hive-image-write-access-config-file  hive-uuid)
  (def reference (hive-image-resolve-ref hive-uuid "master"))
  (hive-image-recompile-gitolite)
  (hive-image-package-key hive-uuid reference)
  (hive-image-packaged? hive-uuid reference)
  (hive-image-package-image hive-uuid reference)
  (hive-image-package-url hive-uuid reference)
  (s3/get-resource-url hive-image-data-bucket "123.zip")
  (s3/get-url hive-image-data-bucket "123.zip")
  (hive-images-sqs-queue)
  (hive-images-send-images-update-message hive-uuid)

  (def incoming-queue (hive-images-sqs-queue))
  (sqs/receive-message config/sqs-aws-credentials
                                     :queue-url incoming-queue
                                     :wait-time-seconds 1
                                     :max-number-of-messages 10
                                     :delete false)
  (def repo (jgit/load-repo "/home/cfitzhugh/working/hivewing-docker"))
  (def repo (jgit/load-repo "/tmp/1418838486719-105021364-1/0b9cf547-c04f-4fe2-949e-94269df0dce7.git"))
  (def repo (jgit/discover-repo "/tmp/1418838486719-105021364-1/0b9cf547-c04f-4fe2-949e-94269df0dce7.git"))

  (def reference "master")
  (def rev-walk (jgit-i/new-rev-walk repo))
  (jgit/git-log repo)
  (jgit/git-status repo)
  (jgit-q/rev-list repo rev-walk)
  (jgit-q/branch-list-with-heads repo rev-walk)
  (jgit-i/resolve-object repo reference)
  (jgit-i/resolve-object repo "ccd7427b8d112923f8434454ceedc7775d001ee1")
)

(def gitolite-shell-command
  (.getPath (io/file (or (env :hivewing-gitolite-shell-command) "/home/git/bin/gitolite"))))

(defn hive-image-recompile-gitolite
  "Run the commands to recompile gitolite"
  []
  (logger/info "Recompiling gitolite...")
  (logger/info "Running " gitolite-shell-command)
  (logger/info (shell/sh gitolite-shell-command "compile"))
  (logger/info (shell/sh gitolite-shell-command "trigger" "POST_COMPILE")))

(defmacro with-gitolite
  "Recompiles gitolite once you make all these changes."
  [& body] `( ~@body (hive-image-recompile-gitolite)))

(def gitolite-root
  (.getPath (io/file (or (env :hivewing-gitolite-root) "/home/git/.gitolite" ))))

(def gitolite-repositories-root
  (.getPath (io/file (or (env :hivewing-gitolite-repositories-root) "/home/git/repositories" ))))

(def gitolite-key-root
  "The place where we'll drop public key files"
  (.getPath (io/file gitolite-root "keydir")))

(def gitolite-conf-root
  "The place where we'll drop repo configuration / hive configuration files"
  (.getPath (io/file gitolite-root "conf" "hives")))

(defn- uuid-split
  "Splits a uuid into a chunked path."
  [uuid]
  (apply io/file (map clojure.string/join (partition 4 (str (crc/crc32 (clojure.string/replace uuid #"-" "")))))))

(defn hive-image-gitolite-beekeeper-name
  "Gives a name to a user in gitolite, so you can reference it in
  gitolite config files"
  [beekeeper-uuid]
  (str "bk-" beekeeper-uuid))

(defn hive-image-access-config-file
  "Create and return the string which represents
   a gitolite config file for a hive"
  [hive-uuid]
    (let [manager-uuids (map hive-image-gitolite-beekeeper-name (map :beekeeper_uuid (hive-manager/hive-managers-get hive-uuid)))
          beekeeper-group-name (str "@hive-" hive-uuid "-beekeepers")]
      ; We want to get all the hive-managers
      ; and their uuids added.
      (clojure.string/join
        "\n"
        [
          "# This is a config file describing the access for the hive's images"
          ""
          (str beekeeper-group-name " = " (clojure.string/join " "  manager-uuids))
          ""
          (str "repo " hive-uuid)
          (str "  C   = " beekeeper-group-name)
          (str "  RW+ = " beekeeper-group-name)
        ]
        )
    ))

(defn hive-image-delete-access-config-file
  [hive-uuid]
  (logger/info "Deleting hive access config file " hive-uuid)
  (let [path (.getPath (io/file gitolite-conf-root (uuid-split hive-uuid) (str hive-uuid ".conf")))]
    (file-utils/recursive-delete (io/file path))))

(defn hive-image-write-access-config-file
  [hive-uuid]
  (logger/info "Writing hive access config file " hive-uuid)
  (let [path (io/file gitolite-conf-root (uuid-split hive-uuid) (str hive-uuid ".conf"))
        file-contents (hive-image-access-config-file hive-uuid)]
          (.mkdirs (io/file (.getParent path)))
          (spit path file-contents)))

(defn hive-image-resolve-ref
  "Resolve the reference / branch to an actual commit hash"
  [hive-uuid reference]
  (jgit/with-repo (io/file gitolite-repositories-root (str hive-uuid ".git"))
    (try
      (let [rev-walk (jgit-i/new-rev-walk repo)
            object   (jgit-i/resolve-object repo reference)]

       ; If it's not found, this throws a nil error
       (jgit-i/bound-commit repo rev-walk object))
       (catch Exception e nil))))

(def hive-image-data-bucket
  (or (env :hivewing-hive-images-bucket-name) "hivewing-images-bucket-undefined"))

(defn ensure-hive-image-bucket
  "Ensures the S3 bucket for images exists"
  [ & opt]

  (if (= opt :delete-first)
    (s3/delete-bucket config/s3-aws-credentials hive-image-data-bucket) )

  (if (not (s3/does-bucket-exist config/s3-aws-credentials hive-image-data-bucket))
    (s3/create-bucket config/s3-aws-credentials hive-image-data-bucket))

   (let [policy {:Version "2012-10-17"
                :Statement [{
                  :Sid "PublicReadGetObject"
                  :Effect "Allow"
                  :Principal "*"
                    :Action ["s3:GetObject"]
                    :Resource [(str "arn:aws:s3:::" hive-image-data-bucket "/*")]}]}
      json (json/write-str policy)]
    (s3/set-bucket-policy config/s3-aws-credentials hive-image-data-bucket json)) )

(defn hive-image-package-key
  "Gets the address that we'll put the image package on in S3."
  [hive-uuid ^org.eclipse.jgit.revwalk.RevCommit reference]
    (str (digest/sha-256 (str hive-uuid (.name reference))) "/" (.name reference)))

(defn hive-image-packaged?
  "Determine if this image package exists on S3 already"
  [hive-uuid reference]
  (let [object-key (hive-image-package-key hive-uuid reference)]
    (try
      (s3/get-object-metadata config/s3-aws-credentials
                              :bucket-name hive-image-data-bucket
                              :key object-key)
      (catch com.amazonaws.AmazonServiceException e false))))

(defn hive-image-encryption-key
  [hive-uuid]
  (digest/sha-512 (str hive-uuid)))

(defn hive-image-encrypt-package
  [hive-uuid input-filename]
  (let [temp-file  (java.io.File/createTempFile (str input-filename) ".enc")]
    (crypto/file-encrypt input-filename (.getPath temp-file) (hive-image-encryption-key hive-uuid))
    temp-file))

(defn hive-image-package-url
  [hive-uuid reference]
  (let [object-key (hive-image-package-key hive-uuid reference)]
    (s3/get-resource-url hive-image-data-bucket object-key)))


(defn hive-image-package-image
  "Packages an image on S3 for the given hive and reference
  Returns the URL to download the packaged image"
  [hive-uuid reference]
  (let [object-key (hive-image-package-key hive-uuid reference)
        temp-file  (java.io.File/createTempFile object-key ".buffer")]

      ; Write all the entries to the zipfile (goes to tmp file)
      (with-open [zip-stream (new ZipOutputStream (new FileOutputStream temp-file))]
        (jgit/with-repo (io/file gitolite-repositories-root (str hive-uuid ".git"))
          (let [tree-walk (jgit-i/new-tree-walk repo reference)]
            (while (.next tree-walk)
              (let [filename (.getPathString tree-walk)
                    git-repository (.getRepository repo)
                    file-stream (.openStream (.open git-repository (jgit/get-blob-id repo reference filename) ))]
                (.putNextEntry zip-stream (new ZipEntry filename))
                (io/copy file-stream zip-stream)
                (.closeEntry zip-stream))))))

      (logger/info "Created hive-image zip file")
      ; Encrypt file
      (let [ encrypted-file (hive-image-encrypt-package hive-uuid (str temp-file))]
        (logger/info "Encrypted hive-image zip file")
        ; Now upload
        ;(with-open [ reader (clojure.java.io/input-stream encrypted-file)]
        (with-open [ reader (clojure.java.io/input-stream temp-file)]
          (logger/info "Uploading hive-image zip file")
          (s3/put-object config/s3-aws-credentials
                         :bucket-name hive-image-data-bucket
                         :key object-key
                         :input-stream reader))
        (.delete encrypted-file))
    (.delete temp-file)
    (let [package-url (hive-image-package-url hive-uuid reference)]
      (logger/info "Uploaded hive-image to " package-url)
      package-url
      )))

(defn hive-image-beekeeper-public-key-name
  "Gives a unique dir for a public-key"
  [beekeeper-uuid pk]
  (str (digest/md5 (str pk)) "/" (hive-image-gitolite-beekeeper-name beekeeper-uuid)))

(defn hive-image-set-beekeeper-public-keys
  "Given a worker-uuid and their public keys
  it will make sure that these are the only ones that
  are available in the gitolite system
  Public_keys are unique across the system by DB design"
  [bk-uuid public-keys]
    (logger/info "gitolite key root: " gitolite-key-root)
    (let [beekeeper-keys-path (.getPath (io/file gitolite-key-root (uuid-split bk-uuid) (str bk-uuid)))]
      (logger/info "BK Keys Path: " beekeeper-keys-path)
      (logger/info "Setting " (count public-keys) " public keys on " bk-uuid)
      ; Delete the existing keys
      (file-utils/recursive-delete (io/file beekeeper-keys-path))

      ; Generate the new keys
      (doseq [public-key public-keys]
        (let [location (io/file beekeeper-keys-path (str (hive-image-beekeeper-public-key-name (str bk-uuid) public-key) ".pub"))]
          (do
            (logger/info "Storing PK in " (.getPath location))
            (.mkdirs (io/file (.getParent location)))
            ; add them to the file system
            (logger/info "Writing out")
            (spit location (str public-key))
            (logger/info "Written to " (.getPath location)))))))
