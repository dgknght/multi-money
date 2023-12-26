(ns multi-money.handler
  (:require [clojure.pprint :refer [pprint]]
            [hiccup.page :refer [html5
                                 include-js]]
            [reitit.core :as r]
            [reitit.ring :as ring]))

(defn- mount-point
  []
  (html5
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]]
    [:body
     [:div#app]
     (include-js "/cljs-out/dev-main.js")]))

(defn- index
  [_req]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (mount-point)})

(defn- test-page [_]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body (html5 [:body [:h1 "This is a test"]])})

(def app
  (ring/ring-handler
    (ring/router
      ["/"
       ["" {:get index}]
       ["test" {:get test-page}]])
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
