(defproject glug "0.1.0-SNAPSHOT"
  :description "Crowdsource Your Alcohol"
  :url "http://mojotech.com/glug"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [ragtime/ragtime.sql.files "0.3.8"]]
  :main ^:skip-aot glug.core
  :plugins [[ragtime/ragtime.lein "0.3.8"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/glug"}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
