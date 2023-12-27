(defproject multi-money "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.11.4"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [clojure.java-time "1.4.2"]
                 [metosin/reitit-ring "0.7.0-alpha7" :exclusions [ring/ring-core ring/ring-codec commons-fileupload crypto-equality commons-io crypto-random commons-codec]]
                 [hiccup "2.0.0-RC2"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [com.github.dgknght/app-lib "0.3.6"
                  :exclusions [ring/ring-core
                               org.clojure/clojure
                               commons-io
                               commons-codec
                               ring
                               ring/ring-devel
                               ring/ring-servlet
                               ring/ring-jetty-adapter]]
                 [reagent "1.1.1" ]]

  :source-paths ["src"]
  :uberjar-name "multi-money.jar"

  :aliases {"fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]
            "fig:test"  ["run" "-m" "figwheel.main" "-co" "test.cljs.edn" "-m" "multi-money.test-runner"]
            "routes"    ["run" "-m" "multi-money.handler/print-routes"]}

  :plugins [[lein-cloverage "1.2.2"]]
  :cloverage {:fail-threshold 90
              :low-watermark 90
              :high-watermark 95}
  :repl-options {:welcome (println "Welcome to accounting with multiple, persistant storage options!")
                 :init-ns multi-money.repl}
  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.17"]
                                  [org.slf4j/slf4j-nop "1.7.30"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]
                   
                   :resource-paths ["target"]
                   ;; need to add the compiled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["target"]}
             :uberjar {;:source-paths ["env/prod/clj"]
                       ;:resource-paths ["env/prod/resources"]
                       :prep-tasks ["compile"
                                    #_["cljsbuild" "once" "min"]
                                    #_"sass"]
                       :env {:production true
                             :app-title "Multimoney"}
                       :aot :all
                       :omit-source true}})
