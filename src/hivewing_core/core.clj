(ns hivewing-core.core)

(defn ensure-uuid
  "Convert to a uuid. If it's nil, we'll just pass the nil through"
  [uuid-maybe]
  (if (or (nil? uuid-maybe) (instance? java.util.UUID uuid-maybe))
    uuid-maybe
    (try
      (java.util.UUID/fromString uuid-maybe)
      (catch java.lang.IllegalArgumentException e
        (throw (ex-info "Invalid UUID" {:type :invalid-uuid}))))))
