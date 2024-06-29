(ns multi-money.models.users
  (:refer-clojure :exclude [find])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [java-time.api :as t]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :as utl :refer [->id
                                              exclude-self]]
            [multi-money.db :as db]))

(declare find-by)

(defn- email-is-unique?
  [u]
  (-> u
      (select-keys [:user/email])
      (exclude-self u)
      find-by
      nil?))
(v/reg-spec email-is-unique? {:message "%s is already in use"
                             :path [:user/email]})
(s/def :user/email v/email?)
(s/def :user/given-name string?)
(s/def :user/surname string?)
(s/def :user/identities (s/map-of keyword? string?))
(s/def ::user (s/and (s/keys :req [:user/email
                                   :user/given-name
                                   :user/surname]
                             :opt-un [::db/id]
                             :opt [:user/identities])
                     email-is-unique?))

(defn- select-identities
  [{:keys [id] :as user}]
  {:pre [(:id user)]}
  (db/select (db/storage)
             {:identity/user-id id}
             {}))

(defn- assoc-identities
  [user]
  ; Some of the data storage strategies embed the identities
  ; in the user record
  (if (:user/identities user)
    user
    (assoc user
           :user/identities
           (->> (select-identities user)
                (map (juxt :identity/oauth-provider :identity/oauth-id))
                (into {})))))

(defn- after-read
  [user]
  (-> user
      assoc-identities
      db/set-meta))

(defn select
  ([criteria] (select criteria {}))
  ([criteria options]
   {:pre [(s/valid? ::db/options options)]}
   (map after-read
        (db/select (db/storage)
                    (db/model-type criteria :user)
                    options))))

(defn find-by
  ([criteria] (find-by criteria {}))
  ([criteria opts]
   (first (select criteria (assoc opts :limit 1)))))

(defn find
  [id]
  (find-by ^{:model-type :user} {:id (->id id)}))

(defn find-by-oauth
  [[oauth-provider id-or-profile]]
  (find-by {:user/identities [:including {:oauth-provider oauth-provider
                                          :oauth-id (->id id-or-profile)}]}))

(defn- yield-or-find
  [m-or-id]
  ; if we have a map, assume it's a model and return it
  ; if we don't, assume it's an ID and look it up
  (if (map? m-or-id)
    m-or-id
    (find m-or-id)))

(defn- resolve-put-result
  [records]
  (some yield-or-find records)) ; This is because when adding a user, identities are inserted first, so the primary record isn't the first one returned

(defn put
  [user]
  (v/with-ex-validation user ::user
    (let [records-or-ids (db/put (db/storage)
                                 [user])]
      (resolve-put-result records-or-ids)))) ; TODO: return all of the saved models instead of the first?

(defn delete
  [user]
  {:pre [user (map? user)]}
  (db/delete (db/storage) [user]))

(defn- +expiration
  [m]
  (assoc m :expires-at (t/to-millis-from-epoch
                         (t/plus (t/instant)
                                 (t/hours 6)))))

(defn- expired?
  [{:keys [expires-at]}]
  (t/before? (t/instant expires-at)
             (t/instant)))

(defn tokenize
  [user]
  {:pre [(:id user)]}
  (+expiration {:user-id (:id user)}))

(defn detokenize
  [{:keys [user-id] :as token}]
  {:pre [(or (nil? token)
             (and (:user-id token)
                  (:expires-at token)))]}

  (when token
    (when-not (expired? token)
      (find user-id))))

(defmulti from-oauth (fn [[provider]] provider))

(defmethod from-oauth :default
  [[provider]]
  (log/errorf "Unrecognized oauth provider %s" provider)
  nil)

(defmethod from-oauth :google
  [[provider profile]]
  (-> profile
      (rename-keys {:given_name :user/given-name
                    :family_name :user/surname
                    :email :user/email})
      (select-keys #{:user/email :user/given-name :user/surname})
      (assoc :user/identities {provider (:id profile)})))
