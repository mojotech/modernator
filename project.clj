(defproject modernator "0.1.0-SNAPSHOT"
  :description "Crowdsource your Life"
  :url "http://github.com/mojotech/modernator"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3123"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]
                 [prismatic/om-tools "0.3.10"]
                 [fuzzy-string "0.1.2-SNAPSHOT"]
                 [cljs-ajax "0.3.10"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [digest "1.4.4"]
                 [clj-time "0.9.0"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.1.4"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring-transit "0.1.3"]
                 [compojure "1.1.6"]
                 [clout "2.1.2"]
                 [com.draines/postal "1.11.3"]
                 [hiccup "1.0.4"]
                 [crypto-random "1.2.0"]
                 [ragtime/ragtime.sql.files "0.3.8"]]
  :main modernator.core
  :plugins [[ragtime/ragtime.lein "0.3.8"]
            [lein-cljsbuild "1.0.5"]
            [lein-sassy "1.0.7"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/modernator"}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :source-paths ["src/clj"]
  :cljsbuild {:builds [{:source-paths ["src/cljs"],
                        :id "main",
                        :compiler
                        {:output-to "resources/public/js/app.js",
                         :asset-path "/static/js/out"
                         :pretty-print true}}]}
  :sass {:src "resources/scss"
         :dst "resources/public/css"})
