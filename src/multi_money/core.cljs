(ns ^:figwheel-hooks multi-money.core
  (:require [clojure.pprint :refer [pprint]]
            [goog.dom :as gdom]
            [accountant.core :as act]
            [secretary.core :as sct]
            [reagent.dom :as rdom]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.bootstrap-5 :as bs]
            [multi-money.state :as state :refer [current-page
                                                 current-user
                                                 current-entities
                                                 +busy
                                                 -busy]]
            [multi-money.views.components :refer [title-bar
                                                  footer]]
            [multi-money.notifications :refer [toasts
                                               alerts]]
            [multi-money.views.pages]
            [multi-money.views.entities]
            [multi-money.views.commodities]
            [multi-money.api.users :as usrs]
            [multi-money.api.entities :as ents]))

(swap! forms/defaults assoc-in [::forms/decoration ::forms/framework] ::bs/bootstrap-5)

(defn get-app-element []
  (gdom/getElement "app"))

(defn full-page []
  (fn []
    [:<>
     [:div.container
      [title-bar]
      [alerts]
      [toasts]
      [@current-page]]
     [footer]]))

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

(defn- fetch-entities []
  (if @state/auth-token
    (do (+busy)
        (ents/select :on-success (fn [res]
                                   (swap! state/app-state assoc
                                          :current-entities res
                                          :current-entity (first res))
                                   (when (empty? res)
                                     (sct/dispatch! "/entities")))
                     :callback -busy))
    (reset! current-entities [])))

(defn- watch-current-user []
  (add-watch current-user
             ::current-user
             (fn [& _]
               (fetch-entities))))

(defn- init! []
  (act/configure-navigation!
    {:nav-handler sct/dispatch!
     :path-exists? sct/locate-route-value})
  (act/dispatch-current!)
  (mount-app-element)
  (watch-current-user)
  (fetch-user)
  (fetch-entities))

(init!)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
