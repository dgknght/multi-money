(ns multi-money.models.users.web
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.api :refer [extract-token-bearer]]
            [ring.util.response :as res]
            [multi-money.tokens :as tkns]
            [multi-money.oauth :refer [fetch-profiles]]
            [multi-money.models.users :as usrs]))

(defn validate-token-and-lookup-user
  [req]
  (let [decoded (-> req
                    extract-token-bearer
                    tkns/decode)]
    (when (= (get-in req [:headers "user-agent"])
             (:user-agent decoded))
      (usrs/detokenize decoded))))

(defn wrap-fetch-oauth-profile
  [handler]
  (fn [{:oauth2/keys [access-tokens] :as req}]
    (handler (let [profiles (fetch-profiles access-tokens)]
               (if (empty? profiles)
                 req
                 (assoc req :oauth2/profiles profiles))))))

(defn wrap-issue-auth-token
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

(defn- find-or-create-user
  [profiles]
  (when (seq profiles)
    (try (or (some usrs/find-by-oauth profiles)
             (some (comp usrs/put
                         usrs/from-oauth)
                   profiles))
         (catch Exception e
           (log/error e "Unable to fetch the user from the oauth profile")))))

(defn wrap-user-lookup
  [handler]
  (fn [{:oauth2/keys [profiles] :as req}]
    (handler (if-let [user (find-or-create-user profiles)]
               (assoc req :authenticated user)
               req))))
