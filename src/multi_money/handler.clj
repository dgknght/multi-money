(ns multi-money.handler
  (:require [clojure.pprint :refer [pprint]]
            [config.core :refer [env]]
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
     (let [filename (format "/cljs-out/%s-main.js"
                            (if (env :production?)
                              "prod"
                              "dev"))]
       (include-js filename))]))

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
