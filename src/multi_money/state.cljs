(ns multi-money.state
  (:require [clojure.pprint :refer [pprint]]
            [reagent.core :as r]
            [reagent.cookies :as cookies]))

(defonce app-state (r/atom {:process-count 0
                            ; TODO: get strategy from the configuration
                            :db-strategy (cookies/get :db-strategy "sql")
                            :auth-token (cookies/get :auth-token)}))

(def current-page (r/cursor app-state [:current-page]))
(def current-user (r/cursor app-state [:current-user]))
(def db-strategy (r/cursor app-state [:db-strategy]))
(def auth-token (r/cursor app-state [:auth-token]))
(def nav-items (r/cursor app-state [:nav-items]))

(defn +busy []
  (swap! app-state update-in [:process-count] inc))

(defn -busy []
  (swap! app-state update-in [:process-count] dec))

(add-watch db-strategy
           ::state
           (fn [_cursor _id _before strategy]
             (pprint {::db-strategy-changed strategy})
             (cookies/set! :db-strategy (name strategy))))
