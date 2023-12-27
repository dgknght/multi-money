(ns multi-money.server
  (:require [clojure.tools.logging :as log]
            [multi-money.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
  (let [port (or (when-let [p (first args)]
                   (Integer/parseInt p))
                 3000)]
    (run-jetty #'app {:port port :join? false})
    (log/infof "Web service is started on port %s." port)))
