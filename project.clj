(defproject multi-money "0.1.0"
   :description "Single-page web app implementation of a double-entry accounting application"
   :url "http://example.com/FIXME"
   :license {:name "Eclipse Public License"
             :url "http://www.eclipse.org/legal/epl-v10.html"}

   :min-lein-version "2.7.1"

   :dependencies [[org.clojure/clojure "1.11.1"]
                  [org.clojure/clojurescript "1.11.4"]
                  [ch.qos.logback/logback-classic "1.2.3"]
                  [com.google.guava/guava "31.0.1-jre"] ; This is here because if I let com.datomic/dev-local resolve its dependencies naturally, I end up with the *-android build instead
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
                  [ring/ring-defaults "0.4.0" :exclusions [ring/ring-core commons-fileupload]]
                  [venantius/accountant "0.2.5"]
                  [clj-commons/secretary "1.2.4"]
                  [com.github.seancorfield/next.jdbc "1.3.909" :exclusions [org.clojure/spec.alpha org.clojure/clojure org.clojure/core.specs.alpha]]
                  [org.postgresql/postgresql "42.6.0" :exclusions [org.checkerframework/checker-qual]]
                  [dev.weavejester/ragtime "0.9.3" :exclusions [org.clojure/spec.alpha org.clojure/clojure org.clojure/core.specs.alpha org.clojure/tools.logging]]
                  [congomongo "2.6.0" :exclusions [org.clojure/data.json]]
                  [commons-io/commons-io "2.16.0"]
                  [com.cognitect/hmac-authn "0.1.211" :exclusions [org.clojure/tools.analyzer org.clojure/tools.analyzer.jvm]]
                  [org.ow2.asm/asm "9.7"]
                  [com.datomic/client-pro "1.0.78"
                   :exclusions [com.cognitect/transit-java
                                com.datomic/client-impl-shared
                                org.eclipse.jetty/jetty-client
                                org.eclipse.jetty/jetty-http
                                org.eclipse.jetty/jetty-io
                                org.eclipse.jetty/jetty-util
                                org.clojure/tools.reader]]
                  [com.datomic/peer "1.0.7075"
                   :exclusions [com.google.errorprone/error_prone_annotations
                                commons-io
                                org.slf4j/slf4j-api
                                org.checkerframework/checker-qual]]
                  [stowaway "0.1.19" :exclusions [potemkin
                                                  org.clojure/data.priority-map]]
                  [com.datomic/client-impl-shared "1.0.102"
                   :exclusions [com.cognitect/transit-java
                                org.eclipse.jetty/jetty-client
                                org.eclipse.jetty/jetty-http
                                org.eclipse.jetty/jetty-io
                                com.cognitect/http-client
                                org.eclipse.jetty/jetty-util
                                org.clojure/tools.reader
                                com.cognitect/transit-clj]]
                  [com.github.dgknght/app-lib "0.3.14"
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
   :repositories [["sonatype" "https://oss.sonatype.org/content/repositories/releases"]]
   :jvm-opts ["-Duser.timezone=UTC"]
   :cloverage {:fail-threshold 90
               :low-watermark 90
               :high-watermark 95
               :ns-exclude-regex [#"multi-money.db.datomic.tasks"
                                  #"multi-money.db.mongo.tasks"
                                  #"multi-money.db.sql.tasks"
                                  #"multi-money.repl"
                                  #"multi-money.server"
                                  #"multi-money.handler"]} ; I'd really like to cover everything except print-routes, but I can't get that working

   :source-paths ["src"]
   :uberjar-name "multi-money.jar"

   :aliases {"fig:build"      ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
             "fig:min"        ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
             "fig:prod"       ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "prod"]
             "fig:test"       ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "multi-money.test-runner"]
             "datomic-schema" ["run" "-m" "multi-money.db.datomic.tasks/apply-schema"]
             "init-mongo"     ["run" "-m" "multi-money.db.mongo.tasks/init"]
             "index-mongo"    ["run" "-m" "multi-money.db.mongo.tasks/index"]
             "create-sql"     ["run" "-m" "multi-money.db.sql.tasks/create"]
             "init-sql"       ["do" "create-sql," "migrate"]
             "migrate"        ["run" "-m" "multi-money.db.sql.tasks/migrate"]
             "rollback"       ["run" "-m" "multi-money.db.sql.tasks/rollback"]
             "remigrate"      ["run" "-m" "multi-money.db.sql.tasks/remigrate"]
             "routes"         ["run" "-m" "multi-money.handler/print-routes"]}

   :repl-options {:welcome (println "Welcome to accounting with multiple, persistent storage options!")
                  :init-ns multi-money.repl}
   :profiles {:dev {:dependencies [[com.datomic/local "1.0.277"
                                    :exclusions [com.cognitect/transit-java
                                                 com.datomic/client
                                                 com.datomic/client-impl-shared
                                                 commons-io
                                                 org.clojure/tools.reader
                                                 com.cognitect/transit-clj]]
                                   [com.bhauman/figwheel-main "0.2.17"
                                    :exclusions [org.slf4j/slf4j-api]]
                                   [com.bhauman/rebel-readline-cljs "0.1.4"]]
                    :source-paths ["env/dev"]
                    :resource-paths ["target" "env/dev/resources" "config/dev"]
                    ;; need to add the compiled assets to the :clean-targets
                    :clean-targets ^{:protect false} ["target"]}
              :test {:source-paths ^:replace ["env/dev" "src"]
                     :resource-paths ^:replace ["target" "env/test/resources" "resources" "config/test"]
                     :dependencies [[com.datomic/local "1.0.277"
                                    :exclusions [com.cognitect/transit-java
                                                 com.datomic/client
                                                 commons-io
                                                 org.clojure/tools.reader
                                                 com.cognitect/transit-clj]]]}
              :util {:resource-paths ^:replace ["target" "resources" "env/prod/resources" "config"]}
              :docker {:local-repo "/root/.m2"}
              :uberjar {:source-paths ["env/prod"]
                        :resource-paths ["env/prod/resources"]
                        :dependencies [[com.bhauman/figwheel-main "0.2.17"]]
                        :prep-tasks ["compile"
                                     "fig:prod"]
                        :aot :all
                        :omit-source true}})
