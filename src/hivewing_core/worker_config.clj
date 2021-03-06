(ns hivewing-core.worker-config
  (:require
            [taoensso.timbre :as logger]
            [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.worker-events :as worker-events]
            [hivewing-core.core :refer [ensure-uuid]]
            [hivewing-core.hive-image :as hi]
            [hivewing-core.pubsub :as pubsub]
            [hivewing-core.postgres-json :as pg-json]
            [clojure.java.jdbc :as jdbc]
            [clojure.data :as clj-data]
            [environ.core  :refer [env]]))
(comment
  (def wu "5e847a7c-b2c9-11e4-ad01-0242ac1100a7")

  (worker-config-set wu {(public-key-key)
      "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCf/VDku3r+GRYCoEk95S5jKFJDhFqp7TRbugCl1Y3cFnCtcVG1qR9OxpbOIsOafI5B2fNhVFB7Xq1t8Hgw1poeZj7NsePSiTLquwr+0p4ISGl/yfYqDBfDdI/VSVdz73iU+L5gwa+d4ERLkKYDWS6isuw7eHf6zJ1YAjOFiDto4Fnh7QDWdwJPrQJD5afAQgEeT8Z1VO8YjXHWLOYcGCObMk5dRKYmRDTnA+ZsMm+0WuYZkgtSjyoUcpDsgbzJKYC86cVG+e2Upw8aw+NSYqFBw4VtZgTTqx1U5iZZ+Qe5FvyqJLQn6hZPC9hV7AfGacPZoL2nUl+BkulNXXEk25kv cfitzhugh@cfitz-server"}
                     :allow-system-keys true)

  (worker-config-get-public-key wu)

  )

(defn tasks-key
  [keyname]
  (str ".tasks." keyname))
(defn tracing-key
  [keyname]
  (str ".tracing." keyname))

(defn public-key-key
  [] ".worker-public-key")

(defn beekeeper-public-keys-key
  [] ".beekeeper-public-keys")

(defn porter-hosts-key [] ".porter-hosts")

(defn worker-uuid-key [] ".worker-uuid")

(defn worker-config-updates-channel
  "Generates the channel worker config updates are on"
  [worker-uuid]
  (str "worker-config:" worker-uuid ":updates"))

(defn worker-config-system-name?
  "Determines if the worker config system name is a system name"
  [name]
  (boolean (re-find #"^\." name)))

(defn worker-config-valid-name?
  "Determines if the worker config is a valid name"
  [name]
  (boolean (re-find #"^((\.)?[a-zA-Z0-9_\-])+" name)))

(defn worker-config-get
  "Given a valid worker-uuid, it will return the configuration 'hash'.
  If you give a non-existent worker-uuid you get back {}
  Pass in :include-system-keys to get back *all* keys (including system ones)"
  [worker-uuid & params]

  (try
    (let [params (apply hash-map params)
          include-system-keys? (:include-system-keys params)
          items (jdbc/query sql-db
                  [(str "SELECT * "
                        " FROM worker_configs "
                        " WHERE worker_uuid = ?")
                   (ensure-uuid worker-uuid)])
        ; Filter out the system keys if needed
          filtered-items (if include-system-keys?
                         items
                         (remove #(worker-config-system-name? (get %1 :key)) items))
        ; Now map those to a hash key structure
          kv-pairs (map #(vector (get % :key) (get % :data)) filtered-items)
          result (into {} kv-pairs)
         ]
      result)
      ;;result)
    (catch clojure.lang.ExceptionInfo e false)))

(defn worker-config-get-public-key
  "Get the worker-config public-key for a worker"
  [worker-uuid]
  (let [
        sql   (str "SELECT * "
                        " FROM worker_configs "
                        " WHERE worker_uuid = ? "
                        " AND key = ? LIMIT 1")
        res (first (jdbc/query sql-db [ sql (ensure-uuid worker-uuid) (public-key-key)]))]
    res))

(defn worker-config-get-tasks
  "Get the tasks for this worker"
  [worker-uuid]
  (let [
        sql   (str "SELECT * "
                        " FROM worker_configs "
                        " WHERE worker_uuid = ? "
                        " AND key LIKE ?")
        items (jdbc/query sql-db [ sql (ensure-uuid worker-uuid) (tasks-key "%")])
        kv-pairs (map #(vector (clojure.string/replace (get % :key) (tasks-key "") "")
                               (get % :data)) items)
        result (into {} kv-pairs) ]
    (or result {})
  ))

(defn worker-config-get-tracing
  [worker-uuid]
  (let [task-names (keys (worker-config-get-tasks worker-uuid))]
    (let [
        sql   (str "SELECT * "
                   " FROM worker_configs "
                   " WHERE worker_uuid = ? "
                   " AND key LIKE ?")

        items (jdbc/query sql-db [ sql (ensure-uuid worker-uuid) (tracing-key "%") ])
        kv-pairs       (map #(vector (clojure.string/replace (get % :key) (tracing-key "") "") true) items)
        from-db (into {} kv-pairs)
        ;; ALl tasks are "false", unless merged and made true
        all-tasks-hash (into {} (map #(vector % (get from-db %)) task-names))
        ]
      all-tasks-hash)))

(defn worker-config-delete
  "Deletes all the keys and such for a given worker. The worker was deleted
  so we should delete!"
  [worker-uuid]
  (jdbc/delete! sql-db :worker_configs ["worker_uuid = ?" (ensure-uuid worker-uuid)]))

(defn worker-config-set
  "Set the configuration on the worker. Provided a uuid and the paramters as a hash.
  The keys for the parameters should be strings or keywords.
  If the param value is nil, we'll delete the key
  Returns true if it worked"
  [worker-uuid parameters & args]
  ; Want to split the parameters

  (let [args (apply hash-map args)
        clean-parameters (if (:allow-system-keys args)
                           ;; If we allow system keys - only needs valid
                           (select-keys parameters (filter #(worker-config-valid-name? %1) (keys parameters)))
                           ;; If we don't allow system keys - needs valid AND not system-name
                           (select-keys parameters (filter #(and (worker-config-valid-name? %1) (not (worker-config-system-name? %1))) (keys parameters))))
        current-params   (worker-config-get worker-uuid :include-system-keys true)
        suppress-change-publication (:suppress-change-publication args)
        ]

    (doseq [[key-name value] clean-parameters]
      (if (nil? value)
        ; Delete this field
        (jdbc/delete! sql-db :worker_configs ["worker_uuid = ? AND LOWER(key) = LOWER(?)" (ensure-uuid worker-uuid) key-name])
        (do
          (let [update (jdbc/update! sql-db :worker_configs
                       {:data value}
                       ["worker_uuid = ? AND LOWER(key) = LOWER(?)"
                        (ensure-uuid worker-uuid) key-name])]
            (if (= 0 (first update))
                   (jdbc/insert! sql-db :worker_configs
                     {:worker_uuid (ensure-uuid worker-uuid)
                      :key (clojure.string/lower-case key-name)
                      :data value}))))))

    (let [[only-current only-new things-in-both] (clojure.data/diff current-params clean-parameters)]
      (if (and (not suppress-change-publication) (not (nil? only-new)))
        (do
          (logger/info "Notifying of changes to worker-config" worker-uuid)
          (pubsub/publish-message (worker-config-updates-channel worker-uuid) clean-parameters))))))

(defn worker-config-set-tracing
  [worker-uuid task state]
    (if state
      ;; Set with start time
      (worker-config-set worker-uuid (hash-map (tracing-key task) (str (.getTime (java.util.Date.)))) :allow-system-keys true)
      ;; Deletes
      (worker-config-set worker-uuid (hash-map (tracing-key task) nil) :allow-system-keys true)))


(defn worker-config-set-hive-image
  [worker-uuid hive-image-url hive-uuid]
    (logger/info (str "Setting hive url " hive-image-url " on worker " worker-uuid " in hive " hive-uuid))
    (worker-config-set worker-uuid {".hive-image" hive-image-url ".hive-image-key" (hi/hive-image-encryption-key hive-uuid)} :allow-system-keys true))
