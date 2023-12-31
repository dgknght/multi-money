(ns ^:figwheel-hooks multi-money.core
  (:require [goog.dom :as gdom]
            [accountant.core :as act]
            [secretary.core :as sct]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [multi-money.views.components :refer [title-bar]]))

; TODO: Move this to state ns
(defonce app-state (r/atom {}))
(def current-page (r/cursor app-state [:current-page]))

; TODO: Move this to pages ns
(defn welcome []
  [:div.container.mt-2
   [:h1 "Welcome!"]
   [:p "We're glad you found us."]])

(sct/defroute welcome-path "/" []
  (reset! current-page welcome))

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

(defn- init! []
  (act/configure-navigation!
    {:nav-handler sct/dispatch!
     :path-exists? sct/locate-route-value})
  (act/dispatch-current!)
  (mount-app-element))

(init!)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
