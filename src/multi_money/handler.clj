(ns multi-money.handler
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [config.core :refer [env]]
            [hiccup.page :refer [html5
                                 include-css
                                 include-js]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
            [ring.middleware.oauth2 :refer [wrap-oauth2]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.json :refer [wrap-json-body
                                          wrap-json-response]]
            [ring.util.response :as res]
            [reitit.core :as r]
            [reitit.ring :as ring]
            [lambdaisland.uri :refer [uri]]
            [dgknght.app-lib.api :refer [wrap-authentication
                                         extract-token-bearer]]
            [multi-money.oauth :refer [fetch-profiles]]
            [multi-money.tokens :as tkns]
            [multi-money.db :refer [with-db]]
            [multi-money.mount-point :refer [js-path]]
            [multi-money.models.users :as usrs]
            [multi-money.api.users :as u]
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
             :schema (:web-server-schema env)
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

(defn- extract-db-strategy
  [req]
  (or (some (comp keyword #(get-in req %))
            [[:headers "db-strategy"]
             [:cookies "db-strategy" :value]])
      (get-in env [:db :active])))

(defn- mask-values
  [m ks]
  (reduce (fn [res k]
            (if (contains? res k)
              (assoc res k "****************")
              res))
          m
          ks))

; TODO: move this to a db.web ns
(defn- wrap-db
  [handler]
  (fn [req]
    (let [storage-key (extract-db-strategy req)
          storage-config (get-in env [:db :strategies storage-key])]
      (log/debugf "Handling request with db strategy %s -> %s"
                  storage-key
                  (mask-values storage-config [:username :user :password]))
      (with-db [storage-config]
        (handler (assoc req :db-strategy storage-key))))))

(defn- find-or-create-user
  [profiles]
  (when (seq profiles)
    (try (or (some usrs/find-by-oauth profiles)
             (some (comp usrs/put
                         usrs/from-oauth)
                   profiles))
         (catch Exception e
           (log/error e "Unable to fetch the user from the oauth profile")))))

; TODO: move this to a users.web ns
(defn- wrap-user-lookup
  [handler]
  (fn [{:oauth2/keys [profiles] :as req}]
    (handler (if-let [user (find-or-create-user profiles)]
               (assoc req :authenticated user)
               req))))

; TODO: move this to a users.web ns
(defn- wrap-issue-auth-token
  [handler]
  (fn [{:keys [authenticated] :as req}]
    (let [cookie-val (when authenticated
                       (-> (usrs/tokenize authenticated)
                           (assoc :user-agent
                                  (get-in req
                                          [:headers "user-agent"]))
                           tkns/encode))
          res (handler req)]
      (cond-> res
        cookie-val (res/set-cookie
                     "auth-token"
                     cookie-val
                     {:same-site :strict
                      :max-age (* 6 60 60)})))))

; TODO: move this to a users.web ns
(defn wrap-fetch-oauth-profile
  [handler]
  (fn [{:oauth2/keys [access-tokens] :as req}]
    (handler (if-let [profiles (seq (fetch-profiles access-tokens))]
               (assoc req :oauth2/profiles profiles)
               req))))

; TODO: move this to a users.web ns
(defn- validate-token-and-lookup-user
  [req]
  (let [decoded (-> req
                    extract-token-bearer
                    tkns/decode)]
    (when (= (get-in req [:headers "user-agent"])
             (:user-agent decoded))
      (usrs/detokenize decoded))))

(def app
  (ring/ring-handler
    (ring/router
      ["/" {:middleware [(wrap-site)
                         wrap-oauth
                         wrap-db
                         wrap-fetch-oauth-profile
                         wrap-user-lookup
                         wrap-issue-auth-token
                         wrap-request-logging]}
       ["" {:get index}]
       ["oauth/*" {:get (constantly {:status 404
                                     :body "not found"})}]
       ["api" {:middleware [[wrap-defaults api-defaults]
                            [wrap-json-body {:keywords? true :bigdecimals? true}]
                            wrap-json-response
                            wrap-db
                            [wrap-authentication {:authenticate-fn validate-token-and-lookup-user}]]}
        u/routes]])
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
