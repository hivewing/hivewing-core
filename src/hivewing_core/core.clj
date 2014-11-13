(ns hivewing-core.core)

(defn ensure-uuid
  [uuid-maybe]
  (if (instance? java.util.UUID uuid-maybe)
    uuid-maybe
    (java.util.UUID/fromString uuid-maybe)))
