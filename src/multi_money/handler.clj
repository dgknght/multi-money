(ns multi-money.handler
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [hiccup.page :refer [html5
                                 include-js]]
            [reitit.core :as r]
            [reitit.ring :as ring]
            [multi-money.mount-point :refer [js-path]]))

(defn- mount-point
  []
  (html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]]
    [:body
     [:div#app]
     (log/debugf "Using javascript resource at %s" js-path)
     (include-js js-path)]))

(defn- index
  [_req]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (mount-point)})

(def app
  (ring/ring-handler
    (ring/router
      ["/"
       ["" {:get index}]])
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler))))

(defn print-routes []
  (pprint
    (map (comp #(take 2 %)
               #(update-in % [1] dissoc :middleware))
         (-> app
             ring/get-router
             r/compiled-routes))))
