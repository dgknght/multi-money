(ns multi-money.handler
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [config.core :refer [env]]
            [hiccup.page :refer [html5
                                 include-css
                                 include-js]]
            [ring.util.response :as res]
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
            [dgknght.app-lib.api :refer [wrap-authentication
                                         wrap-api-exception]]
            [multi-money.db.web :refer [wrap-db
                                        wrap-auth-config]]
            [multi-money.models.users.web :refer [validate-token-and-lookup-user
                                                  wrap-fetch-oauth-profile
                                                  wrap-issue-auth-token
                                                  wrap-user-lookup]]
            [multi-money.config :as config]
            [multi-money.mount-point :refer [js-path]]
            [multi-money.api.users :as usrs]
            [multi-money.api.entities :as ents]
            [multi-money.api.commodities :as cdts]
            [multi-money.api.accounts :as acts]
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
     (config/script)
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
    (log/tracef "Request: %s" (with-out-str (pprint (dissoc req ::r/match ::r/router))))
    (let [res (handler req)]
      (log/infof "Responded to %s \"%s\": %s"
                 (:request-method req)
                 (:uri req)
                 (:status res))
      (log/tracef "Response: %s" (with-out-str (pprint res)))
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

; It seems the browser doesn't beleive us until we've done it twice
(defn- redirect-for-signout
  [url]
  (fn [_]
    (-> (res/redirect url)
        (res/set-cookie :ring-session "" {:max-age 0})
        (res/set-cookie :auth-token   "" {:max-age 0})
        (assoc :session {:ring.middleware.oauth2/access-tokens nil}))))

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
        ["sign-out" {:get {:handler (redirect-for-signout "/signed-out")}}]
        ["signed-out" {:get {:handler (redirect-for-signout "/")}}]
        ["oauth/*" {:get (constantly {:status 404
                                      :body "not found"})}]]
       ["/api" {:middleware [wrap-request-logging
                             [wrap-defaults (assoc-in api-defaults [:security :anti-forgery] false)]
                             [wrap-json-body {:keywords? true :bigdecimals? true}]
                             wrap-json-response
                             wrap-api-exception
                             wrap-auth-config
                             wrap-db
                             [wrap-authentication {:authenticate-fn validate-token-and-lookup-user}]]}
        usrs/routes
        ents/routes
        cdts/routes
        acts/routes]])
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
