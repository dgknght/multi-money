(ns multi-money.db.sql.users
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql])
  (:import java.util.UUID))

(defmethod sql/attributes :user [_]
  [:id :username :email :given-name :surname])

(defn- inflate-identity
  [user-id [p id]]
  #:identity{:user-id user-id
             :oauth-provider p
             :oauth-id id})

(defmethod sql/deconstruct :user
  [{:as user :user/keys [identities]}]
  (let [id (or (:id user) (UUID/randomUUID))]
    (-> user
        (assoc :id id)
        (dissoc :user/identities)
        (cons (map (partial inflate-identity id)
                   identities)))))

(defmethod sql/prepare-criteria :user
  [{:as criteria :user/keys [identities]}]
  ; Identities should look like this:
  ; [:= [:google "abc123"]]
  (if (seq identities)
    (let [[_ [oauth-provider oauth-id]] identities]
      (-> criteria
          (dissoc :user/identities)
          (assoc [:identity :identity/oauth-provider] oauth-provider
                 [:identity :identity/oauth-id] oauth-id)
          (db/model-type :user)))
    criteria))

(defmethod sql/resolve-temp-ids :identity
  [ident id-map]
  (update-in ident [:identity/user-id] id-map))
