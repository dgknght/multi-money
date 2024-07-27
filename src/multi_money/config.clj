(ns multi-money.config
  (:require [config.core :refer [env]]
            [cheshire.core :as json]))

(def ^:private public-config-keys
  [:app-name])

(defn script
  "Builds a hiccup script element defining JSON map of the config that is to be made
  available on the client."
  []
  [:script "window.CONFIG = " (json/generate-string (select-keys env public-config-keys)) ";"])
