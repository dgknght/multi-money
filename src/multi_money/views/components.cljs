(ns multi-money.views.components
  (:require [multi-money.icons :refer [icon]]))

(defn title-bar []
  (fn []
    [:nav.navbar.navbar-expand-lg.bg-body-tertiary.rounded.mt-1
     {:aria-label "Primary Navigation Menu"}
     [:div.container-fluid
      [:a.navbar-brand {:href "/"}
       (icon :cash-stack :size :large)]
      [:button.navbar-toggler {:type :button
                               :data-bs-toggle :collapse
                               :data-bs-target "#primary-nav"
                               :aria-controls "primary-nav"
                               :aria-expanded false
                               :aria-label "Toggle Navigation"}
       [:span.navbar-toggler-icon]]
      [:div#primary-nav.collapse.navbar-collapse
       [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
        [:li.nav-item
         [:a.nav-link {:href "/other-page"} "Other Page"]]]]]]))
