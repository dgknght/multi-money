(ns multi-money.server
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :refer [parse-opts]]
            [ring.adapter.jetty :refer [run-jetty]]
            [multi-money.handler :refer [app]])
  (:gen-class))

(def ^:private opt-specs
  [["-p" "--port PORT" "The port on which the service will listen for requests"
    :id :port
    :parse-fn #(Integer/parseInt %)
    :default 3000]])

(defn- write-errors
  [{:keys [errors summary]}]
  (log/errorf "Unable to start the service due to the following errors: %s"
              errors)
  (log/infof "Usage: %s" summary))

(defn- launch-service
  [{:keys [port]}]
  (log/debugf "Starting web service on port %s." port)
  (run-jetty #'app {:port port :join? false})
  (log/infof "Web service is started on port %s." port))

(defn -main [& args]
  (let [{:keys [options errors] :as parsed} (parse-opts args opt-specs)]
    (if (seq errors)
      (write-errors parsed)
      (launch-service options))))
