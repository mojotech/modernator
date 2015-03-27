(defproject glug "0.1.0-SNAPSHOT"
  :description "Crowdsource Your Alcohol"
  :url "http://mojotech.com/glug"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3123"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]
                 [throttler "1.0.0"]
                 [org.clojure/data.json "0.2.5"]
                 [http-kit "2.1.16"]
                 [cljs-ajax "0.3.10"]
                 [prismatic/om-tools "0.3.10"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.1.4"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring-transit "0.1.3"]
                 [compojure "1.1.6"]
                 [com.draines/postal "1.11.3"]
                 [hiccup "1.0.4"]
                 [garden "1.2.5"]
                 [crypto-random "1.2.0"]
                 [ragtime/ragtime.sql.files "0.3.8"]]
  :main ^:skip-aot glug.core
  :plugins [[ragtime/ragtime.lein "0.3.8"]
            [lein-cljsbuild "1.0.5"]
            [lein-garden "0.2.5"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/glug"}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :source-paths ["src/clj"]
  :garden {:builds [{:id "screen"
                     :source-paths ["src/clj/glug/assets"]
                     :stylesheet glug.assets.styles/screen
                     :compiler {:output-to "resources/public/css/main.css"
                                :pretty-print? false}}]}
  :cljsbuild {:builds [{:source-paths ["src/cljs"],
                        :id "main",
                        :compiler
                        {:output-to "resources/public/js/app.js",
                         :asset-path "/static/js/out"
                         :pretty-print true}}]})
