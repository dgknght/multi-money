(ns multi-money.repl
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [multi-money.handler :refer [app]]))

(def server (atom nil))

(defn start-server []
  (if @server
    (println "A server is already running. Stop it with stop-server.")
    (do
      (reset! server (run-jetty app {:port 3000
                                     :open-browser? false
                                     :join? false}))
      (println "The service is now running on port 3000."))))

(defn stop-server []
  (if-let [s @server]
    (do
      (.stop s)
      (reset! server nil)
      (println "The service has been stopped"))
    (println "No service is running.")))
