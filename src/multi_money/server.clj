(ns multi-money.server
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [multi-money.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(def ^:private opt-specs
  [["-p" "--port PORT" "The port on which the service will listen for requests"
    :id :port
    :parse-fn #(Integer/parseInt %)
    :default 3000]])

(defn- write-errors
  [{:keys [errors summary]}]
  (println "ERROR")
  (doseq [e errors]
    (println (format "  %s" e)))
  (println "")
  (println "USAGE:")
  (println summary))

(defn- launch-service
  [{:keys [port]}]
  (run-jetty #'app {:port port :join? false})
  (log/infof "Web service is started on port %s." port))

(defn -main [& args]
  (let [{:keys [options errors] :as parsed} (parse-opts args opt-specs)]
    (if (seq errors)
      (write-errors parsed)
      (launch-service options))))
