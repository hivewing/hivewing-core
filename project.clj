(defproject hivewing-core "0.1.0-SNAPSHOT"
  :description "Internal 'min' layer for Hivewing"
  :url "http://core.hivewing.io"
  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]
                 [rotary "0.4.1"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.1-901.jdbc4"]
                 ]
  :plugins [[lein-environ "1.0.0"]]
  )
