(defproject hivewing-core "0.1.0-SNAPSHOT"
  :description "Internal 'min' layer for Hivewing"
  :url "http://core.hivewing.io"
  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]
                 [rotary "0.4.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [postgresql "9.1-901.jdbc4"]
                 [ragtime "0.3.6"]    ; needs to be 0.3.6 - stored functions dont work in 0.3.7
                 [com.cemerick/bandalore "0.0.6" :exclusions [joda-time]]
                 [com.taoensso/carmine "2.7.1"]
                 ]
  :plugins [[lein-environ "1.0.0"]
            [ragtime/ragtime.lein "0.3.6"]
            [s3-wagon-private "1.1.2"]]
  :aliases {"seed" ["run" "-m" "hivewing-core.seed/seed-database"]}
  :ragtime {:migrations ragtime.sql.files/migrations}
  :repositories [["hivewing-core" {:url "s3p://clojars.hivewing.io/hivewing-core/releases"
                             :username "AKIAJFFI52CFHPRZIPJA"
                             :passphrase "THLlPQGSstzspARpgw4s+LgqFT2DyrNFJ+vxkHDf"}]]
  )
