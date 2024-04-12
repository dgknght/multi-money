(ns multi-money.server
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [ring.adapter.jetty :refer [run-jetty]]
            [config.core :refer [env]]
            [multi-money.util :refer [mask-values]]
            [multi-money.handler :refer [app]])
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
  (log/debugf "Starting web service on port %s." port)
  (run-jetty #'app {:port port :join? false})
  (log/infof "Web service is started on port %s." port))

(defn -main [& args]

  (pprint {::config (mask-values env :google-oauth-client-id :google-oauth-client-secret)})
  (let [f (io/as-file ".")]
    (pprint {::root f
           ::dir (.list f)}))

  (let [{:keys [options errors] :as parsed} (parse-opts args opt-specs)]
    (if (seq errors)
      (write-errors parsed)
      (launch-service options))))
