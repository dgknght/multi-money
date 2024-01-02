(ns multi-money.models.users
  (:refer-clojure :exclude [find])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [clojure.set :refer [rename-keys]]
            [clj-time.core :as t]
            [clj-time.coerce :refer [to-long
                                     from-long]]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :as utl :refer [->id valid-id?]]
            [multi-money.db :as db]))

(s/def :user/id valid-id?)
(s/def :user/username string?)
(s/def :user/email v/email?)
(s/def :user/given-name string?)
(s/def :user/surname string?)
(s/def :user/identities (s/map-of keyword? string?))
(s/def ::user (s/keys :req [:user/username
                            :user/email
                            :user/given-name
                            :user/surname]
                      :opt [:user/identities]))

(s/def ::criteria (s/keys :opt [:user/email
                                :user/username
                                :user/id]))

(defn- select-identities
  [user]
  (db/select (db/storage)
              {:identity/user-id (:user/id user)}
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
                (map (juxt :provider :provider-id))
                (into {})))))
(defn- after-read
  [user]
  (-> user
      assoc-identities
      (db/set-meta :user)))

(defn select
  ([criteria] (select criteria {}))
  ([criteria options]
   {:pre [(s/valid? ::criteria criteria)
          (s/valid? ::db/options options)]}
   (map (comp after-read
              #(utl/qualify-keys % :user))
        (db/select (db/storage)
                    (db/model-type criteria :user)
                    options))))

(defn find-by
  ([criteria] (find-by criteria {}))
  ([criteria opts]
   (first (select criteria (assoc opts :limit 1)))))

(defn find
  [id]
  (find-by {:user/id (->id id)}))

(defn- resolve-put-result
  [records]
  (some find records)) ; This is because when adding a user, identities are inserted first, so the primary record isn't the first one returned

(defn put
  [user]
  {:pre [user (s/valid? ::user user)]}
  (let [records-or-ids (db/put (db/storage)
                               [(dissoc user :user/identities)])]
    (resolve-put-result records-or-ids))) ; TODO: return all of the saved models instead of the first?

(defn delete
  [user]
  {:pre [user (map? user)]}
  (db/delete (db/storage) [user]))

(defn- +expiration
  [m]
  (assoc m :expires-at (to-long
                         (t/plus (t/now)
                                 (t/hours 6)))))

(defn- expired?
  [{:keys [expires-at]}]
  (t/before? (from-long expires-at)
             (t/now)))

(defn tokenize
  [user]
  {:pre [(:id user)]}
  (+expiration {:user-id (:id user)}))

(defn detokenize
  [{:keys [user-id] :as token}]
  {:pre [(:user-id token)]}
  (when token
    (when-not (expired? token)
      (find user-id))))

(defmulti create-from-oauth (fn [[provider]] provider))

(defmethod create-from-oauth :default
  [[provider]]
  (log/errorf "Unrecognized oauth provider %s" provider)
  nil)

(defn find-by-oauth
  [[provider id-or-profile]]
  (find-by {:user/identities [:= [provider (or (:id id-or-profile)
                                               id-or-profile)]]}))

(defmethod create-from-oauth :google
  [[provider profile]]
  (-> profile
      (rename-keys {:given_name :given-name
                    :family_name :surname})
      (select-keys [:email :given-name :surname])
      (assoc :user/identities {provider (:id profile)})
      put))
