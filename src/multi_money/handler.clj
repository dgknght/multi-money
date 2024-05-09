(ns multi-money.handler
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [config.core :refer [env]]
            [hiccup.page :refer [html5
                                 include-css
                                 include-js]]
            [ring.middleware.defaults :refer [wrap-defaults
                                              site-defaults
                                              api-defaults]]
            [ring.middleware.oauth2 :refer [wrap-oauth2]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.json :refer [wrap-json-body
                                          wrap-json-response]]
            [reitit.core :as r]
            [reitit.ring :as ring]
            [lambdaisland.uri :refer [uri]]
            [dgknght.app-lib.api :refer [wrap-authentication]]
            [multi-money.db.web :refer [wrap-db]]
            [multi-money.models.users.web :refer [validate-token-and-lookup-user
                                                  wrap-fetch-oauth-profile
                                                  wrap-issue-auth-token
                                                  wrap-user-lookup]]
            [multi-money.mount-point :refer [js-path]]
            [multi-money.api.users :as usrs]
            [multi-money.api.entities :as ents]
            [multi-money.db.datomic.ref]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]))

(defn- mount-point
  []
  (html5
    {:lang "en"
     :data-bs-theme :dark}
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     [:link {:rel :icon
             :href "images/cash-coin.png"}]
     (include-css "css/site.css")
     (include-js "https://unpkg.com/@popperjs/core@2")
     (include-js "js/bootstrap.min.js")]
    [:body
     [:div#app]
     (log/debugf "Using javascript resource at %s" js-path)
     (include-js js-path)]))

(defn- index
  [_req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (mount-point)})

(defn- wrap-request-logging
  [handler]
  (fn [req]
    (log/infof "Request received %s \"%s\""
               (:request-method req)
               (:uri req))
    (let [res (handler req)]
      (log/infof "Responded to %s \"%s\": %s"
                 (:request-method req)
                 (:uri req)
                 (:status res))
      res)))

(defn- landing-uri []
  (-> (uri "/")
      (assoc :host   (:web-server-host env)
             :scheme (:web-server-scheme env)
             :port   (:web-server-port env))
      str))

(def ^:private wrap-oauth
  [wrap-oauth2
   {:google
    {:authorize-uri "https://accounts.google.com/o/oauth2/v2/auth"
     :access-token-uri "https://www.googleapis.com/oauth2/v4/token"
     :client-id (env :google-oauth-client-id)
     :client-secret (env :google-oauth-client-secret)
     :scopes ["email" "profile"]
     :launch-uri "/oauth/google"
     :redirect-uri "/oauth/google/callback"
     :landing-uri (landing-uri)}}])

(defn- wrap-site []
  (let [c-store (cookie-store)]
    [wrap-defaults (update-in site-defaults
                              [:session]
                              merge
                              {:store c-store
                               :cookie-attrs {:same-site :lax
                                              :http-only true}})]))

(def app
  (ring/ring-handler
    (ring/router
      [["/" {:middleware [(wrap-site)
                          wrap-oauth
                          wrap-db
                          wrap-fetch-oauth-profile
                          wrap-user-lookup
                          wrap-issue-auth-token
                          wrap-request-logging]}
        ["" {:get index}]
        ["oauth/*" {:get (constantly {:status 404
                                      :body "not found"})}]]
       ["/api" {:middleware [[wrap-defaults (assoc-in api-defaults [:security :anti-forgery] false)]
                             [wrap-json-body {:keywords? true :bigdecimals? true}]
                             wrap-json-response
                             wrap-db
                             [wrap-authentication {:authenticate-fn validate-token-and-lookup-user}]]}
        usrs/routes
        ents/routes]])
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
