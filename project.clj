(defproject hivewing-core "0.1.3-SNAPSHOT"
  :description "Internal 'min' layer for Hivewing"
  :url "http://core.hivewing.io"
  :dependencies [
                 [org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [postgresql "9.1-901.jdbc4"]
                 [ragtime "0.3.6"]    ; needs to be 0.3.6 - stored functions dont work in 0.3.7
                 [com.taoensso/timbre "3.3.1"]
                 [amazonica "0.3.3" :exclusions [joda-time]]
                 [joda-time "2.2"]
                 [digest "1.4.4"]
                 [biscuit "1.0.0"]
                 [clj-jgit "0.8.2"]
                 [clojure-tools "1.1.2"]
                 [org.clojars.runa/conjure "2.1.3"]
                 [commons-lang                                "2.5"]
                 [commons-io/commons-io                       "2.4"]
                 [commons-codec                               "1.7"]
                 [com.taoensso/carmine "2.7.1"]
                ]
  :plugins [[lein-environ "1.0.0"]
            [ragtime/ragtime.lein "0.3.6"]
            [s3-wagon-private "1.1.2"]]
  :aliases {"seed" ["run" "-m" "hivewing-core.seed/seed-database"]
            "setup-aws" ["run" "-m" "hivewing-core.seed/setup-aws"]}
  :ragtime {:migrations ragtime.sql.files/migrations}
  :repositories [["hivewing-core" {:url "s3p://clojars.hivewing.io/hivewing-core/releases"
                                   :sign-releases false
                             :username "AKIAJFFI52CFHPRZIPJA"
                             :passphrase "THLlPQGSstzspARpgw4s+LgqFT2DyrNFJ+vxkHDf"}]]
  )
