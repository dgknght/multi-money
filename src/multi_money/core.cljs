(ns ^:figwheel-hooks multi-money.core
  (:require [clojure.pprint :refer [pprint]]
            [goog.dom :as gdom]
            [accountant.core :as act]
            [secretary.core :as sct]
            [reagent.dom :as rdom]
            [multi-money.state :as state :refer [current-page
                                                 current-user
                                                 +busy
                                                 -busy]]
            [multi-money.views.components :refer [title-bar]]
            [multi-money.views.pages]
            [multi-money.api.users :as usrs]))

(defn get-app-element []
  (gdom/getElement "app"))

(defn full-page []
  (fn []
    [:div.container
     [title-bar] 
     [@current-page]]))

(defn mount [el]
  (rdom/render [full-page] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

(defn- fetch-user []
  (when @state/auth-token
    (+busy)
    (usrs/me :on-success #(reset! current-user %)
             :callback -busy)))

(defn- init! []
  (act/configure-navigation!
    {:nav-handler sct/dispatch!
     :path-exists? sct/locate-route-value})
  (act/dispatch-current!)
  (mount-app-element)
  (fetch-user))

(init!)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
