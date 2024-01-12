(defproject multi-money "0.1.0"
   :description "Single-page web app implementation of a double-entry accounting application"
   :url "http://example.com/FIXME"
   :license {:name "Eclipse Public License"
             :url "http://www.eclipse.org/legal/epl-v10.html"}

   :min-lein-version "2.7.1"

   :dependencies [[org.clojure/clojure "1.10.0"]
                  [org.clojure/clojurescript "1.11.4"]
                  [ch.qos.logback/logback-classic "1.2.3"]
                  [cljsjs/react "17.0.2-0"]
                  [cljsjs/react-dom "17.0.2-0"]
                  [clojure.java-time "1.4.2"]
                  [metosin/reitit-ring "0.7.0-alpha7" :exclusions [ring/ring-core ring/ring-codec commons-fileupload crypto-equality commons-io crypto-random commons-codec]]
                  [hiccup "2.0.0-RC2"]
                  [ring-oauth2 "0.2.2" :exclusions [ring/ring-core commons-fileupload]]
                  [buddy/buddy-sign "3.5.351" :exclusions [org.clojure/spec.alpha org.clojure/clojure commons-codec org.clojure/core.specs.alpha]]
                  [yogthos/config "1.2.0" :exclusions [org.clojure/spec.alpha org.clojure/clojure org.clojure/core.specs.alpha]]
                  [com.andrewmcveigh/cljs-time "0.5.2"]
                  [ring/ring-core "1.9.6"]
                  [ring/ring-jetty-adapter "1.9.6"]
                  [ring/ring-json "0.5.1"]
                  [ring/ring-mock "0.4.0"]
                  [venantius/accountant "0.2.5"]
                  [clj-commons/secretary "1.2.4"]
                  [com.github.seancorfield/next.jdbc "1.3.909" :exclusions [org.clojure/spec.alpha org.clojure/clojure org.clojure/core.specs.alpha]]
                  [org.postgresql/postgresql "42.6.0"]
                  [dev.weavejester/ragtime "0.9.3" :exclusions [org.clojure/spec.alpha org.clojure/clojure org.clojure/core.specs.alpha org.clojure/tools.logging]]
                  [com.github.dgknght/app-lib "0.3.6"
                   :exclusions [ring/ring-core
                                org.clojure/clojure
                                commons-io
                                commons-codec
                                ring
                                ring/ring-devel
                                ring/ring-servlet
                                ring/ring-jetty-adapter]]
                  [reagent "1.1.1" ]
                  [reagent-utils "0.3.3"]]
   :plugins [[lein-cloverage "1.2.2"]]
   :cloverage {:fail-threshold 90
               :low-watermark 90
               :high-watermark 95
               :ns-exclude-regex [#"multi-money.db.sql.migrations"
                                  #"multi-money.repl"
                                  #"multi-money.server"
                                  #"multi-money.handler"]} ; I'd really like to cover everything except print-routes, but I can't get that working

   :source-paths ["src"]
   :uberjar-name "multi-money.jar"

   :aliases {"fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
             "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
             "fig:prod"  ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "prod"]
             "fig:test"  ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "multi-money.test-runner"]
             "migrate"   ["run" "-m" "multi-money.db.sql.migrations/migrate"]
             "rollback"  ["run" "-m" "multi-money.db.sql.migrations/rollback"]
             "remigrate" ["run" "-m" "multi-money.db.sql.migrations/remigrate"]
             "routes"    ["run" "-m" "multi-money.handler/print-routes"]}

   :repl-options {:welcome (println "Welcome to accounting with multiple, persistent storage options!")
                  :init-ns multi-money.repl}
   :profiles {:dev [:project/dev :profiles/dev]
              :project/dev {:dependencies [[com.bhauman/figwheel-main "0.2.17" :exclusions [org.slf4j/slf4j-api]]
                                           [com.bhauman/rebel-readline-cljs "0.1.4"]]
                            :source-paths ["env/dev"]
                            :resource-paths ["target" "env/dev/resources" "config/dev"]
                            ;; need to add the compiled assets to the :clean-targets
                            :clean-targets ^{:protect false} ["target"]}
              :test [:project/test :profiles/test]
              :project/test {:source-paths ^:replace ["env/dev" "src"]
                             :resource-paths ^:replace ["target" "env/test/resources" "resources" "config/test"]}
              :uberjar {:source-paths ["env/prod"]
                        :resource-paths ["env/prod/resources" "config/prod"]
                        :dependencies [[com.bhauman/figwheel-main "0.2.17"]]
                        :prep-tasks ["compile"
                                     "fig:prod"]
                        :aot :all
                        :omit-source true}})
