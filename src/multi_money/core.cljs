(ns ^:figwheel-hooks multi-money.core
  (:require [goog.dom :as gdom]
            [accountant.core :as act]
            [secretary.core :as sct]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [dgknght.app-lib.html :as html]
            [multi-money.views.components :refer [title-bar]]))

; TODO: Move this to state ns
(defonce app-state (r/atom {}))
(def current-page (r/cursor app-state [:current-page]))

; TODO: Move this to pages ns
(defn- sign-in-options []
  [:div.list-group {:style {:max-width "264px"}}
   [:a.list-group-item.list-group-item-action.d-flex.align-items-center.p-0
    {:href "/oauth/google"
     :style {:background-color "#4285F4"
             :font-weight :bold}}
    [:div.bg-light.p-2.rounded
     (html/google-g)]
    [:div.text-center.w-100 "Sign In With Google"]]])

(defn- welcome []
  (fn []
    ; Unsigned-in version
    [:div.container
     [:h1 "Welcome!"]
     [:p "There's lots of cool stuff coming soon."]
     (sign-in-options)]
    #_(if @current-user
        ; Signed-in version
        [:div.container
         [:h1 (format "Welcome %s!" (:given-name @current-user))]
         [:p "Soon we'll put a dashboard here that shows highlights of your finances."]])))

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
