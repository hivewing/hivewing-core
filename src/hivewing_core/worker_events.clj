(ns hivewing-core.worker-events
  (:require [hivewing-core.configuration :refer [sql-db]]
            [hivewing-core.core :refer [ensure-uuid]]
            [clojure.set :as clj-set]
            [clojure.string :as clj-string]
            [hivewing-core.pubsub :as pubsub]
            [clojure.java.jdbc :as jdbc]))

(defn worker-events-system-name?
  "Determines if the worker event name is a system event name"
  [name]
  (boolean (re-find #"^\." name)))

(defn worker-events-valid-name?
  "Determines if the worker event has a valid name"
  [name]
  (boolean (re-find #"^((\.)?[a-zA-Z0-9_\-])+" name)))

(defn worker-events-channel
  "Generates the channel worker events are sent on"
  [worker-uuid]
  (str "worker:" worker-uuid ":events"))

(defn worker-events-send
  "Sends the worker events"
  [ worker-uuid & args ]
  (pubsub/publish-message (worker-events-channel worker-uuid) (apply hash-map args)))
