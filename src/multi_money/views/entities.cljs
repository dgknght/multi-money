(ns multi-money.views.entities
  (:require [secretary.core :as sct]
            [multi-money.state :refer [current-page]]))

(defn- index []
  (fn []
    [:div.container
     [:h1.mt-3 "Entities"]]))

(sct/defroute entities-path "/entities" []
  (reset! current-page index))
