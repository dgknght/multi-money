(ns multi-money.state
  (:require [reagent.core :as r]))

(defonce app-state (r/atom {:process-count 0}))
(def current-page (r/cursor app-state [:current-page]))
(def current-user (r/cursor app-state [:current-user]))
(def auth-token (r/cursor app-state [:auth-token]))

(defn +busy []
  (swap! app-state update-in [:process-count] inc))

(defn -busy []
  (swap! app-state update-in [:process-count] dec))

