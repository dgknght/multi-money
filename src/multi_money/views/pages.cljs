(ns multi-money.views.pages
  (:require [secretary.core :as sct]
            [dgknght.app-lib.html :as html]
            [goog.string :refer [format]]
            [multi-money.state :refer [current-page
                                       current-user]]))

(defn- sign-in-options []
  [:div.list-group {:style {:max-width "264px"}}
   [:a.list-group-item.list-group-item-action.d-flex.align-items-center.p-0
    {:href "/oauth/google"
     :style {:background-color "#4285F4"
             :font-weight :bold}}
    [:div.bg-light.p-2.rounded
     (html/google-g)]
    [:div.text-center.w-100 "Sign In With Google"]]])

(defn welcome []
  (fn []
    (if @current-user
      [:div.container
       [:h1.mt-3 (format "Welcome %s!" (:user/given-name @current-user))]
       [:p "Soon we'll put a dashboard here that shows highlights of your finances."]]
      [:div.container
       [:h1.mt-3 "Welcome!"]
       [:p "There's lots of cool stuff coming soon."]
       (sign-in-options)])))

(sct/defroute welcome-path "/" []
  (reset! current-page welcome))
