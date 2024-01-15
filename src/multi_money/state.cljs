(ns multi-money.state
  (:require [reagent.core :as r]
            [reagent.cookies :as cookies]))

(defonce app-state (r/atom {:process-count 0
                            ; TODO: get strategy from the configuration
                            :db-strategy (keyword (cookies/get :db-strategy "sql"))
                            :auth-token (cookies/get :auth-token)}))

(def current-page (r/cursor app-state [:current-page]))
(def current-user (r/cursor app-state [:current-user]))
(def auth-token (r/cursor app-state [:auth-token]))

(defn +busy []
  (swap! app-state update-in [:process-count] inc))

(defn -busy []
  (swap! app-state update-in [:process-count] dec))

