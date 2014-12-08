(ns hivewing-core.seed
  (:require [hivewing-core.worker       :as worker]
            [hivewing-core.hive         :as hive]
            [hivewing-core.apiary       :as apiary]
            [hivewing-core.beekeeper    :as bk]
            [hivewing-core.hive-manager :as hm]))

(defn seed-beekeeper
  [email]
  (let [keeper (bk/beekeeper-find-by-email email)]
    (if (nil? keeper)
      (let
        [keeper (bk/beekeeper-create {:email email})
         apiary (apiary/apiary-create {:beekeeper_uuid (:uuid keeper)})
         hive   (hive/hive-create {:name "Default Hive" :apiary_uuid (:uuid apiary)})
         hive-manager (hm/hive-manager-create (:uuid hive) (:uuid keeper) :can_write true)
         worker (worker/worker-create {:apiary_uuid (:uuid apiary) :hive_uuid (:uuid hive)})
        ]
        (println "Created " email))))
  )

(defn print-beekeeper
  "Print the details / access etc for a beekeeper"
  [email]
  (let [keeper (bk/beekeeper-find-by-email email)
        apiary (apiary/apiary-find-by-beekeeper (:uuid keeper))
        hive   (first (apiary/apiary-get-hives (:uuid apiary)))
        hive-manager (hm/hive-managers-get (:uuid hive))
        workers (worker/worker-list (:uuid hive))
        tokens (map #(select-keys (worker/worker-get (:uuid %1) :include-access-token true) [:uuid :access_token] ) workers )
        ]
    (println "---------------------------------------------------------------------------------------------")
    (println "User: " keeper)
    (println)
    (println "Apiary: " apiary)
    (println)
    (println "Hive: " hive)
    (println)
    (println "Hive-Manager: " hive-manager)
    (println )
    (println "Workers: " workers)
    (println "tokens " tokens)
    (println )
    (println "---------------------------------------------------------------------------------------------")
    ))

(defn seed-database
  "Add some data to the DB and print out the details"
  []
  (doseq [email ["tim@hivewing.io" "cary@hivewing.io"]]
    (do
        (seed-beekeeper email)
        (print-beekeeper email))
    ))
