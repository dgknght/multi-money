(ns multi-money.db.sql.users
  (:require [multi-money.db :as db]
            [multi-money.db.sql :as sql])
  (:import java.util.UUID))

(defmethod sql/attributes :user [_]
  [:id :email :given-name :surname])

(defn- inflate-identity
  [user-id [p id]]
  (db/model-type {:user-id user-id
                  :provider p
                  :provider-id id}
                 :identity))

(defmethod sql/deconstruct :user
  [{:as user :keys [identities]}]
  (let [id (or (:id user) (UUID/randomUUID))]
    (-> user
        (assoc :id id)
        (dissoc :identities)
        (cons (map (partial inflate-identity id)
                   identities)))))

(defmethod sql/prepare-criteria :user
  [{:as criteria :keys [identities]}]
  ; Identities should look like this:
  ; [:= [:google "abc123"]]
  (if (seq identities)
    (let [[_ [oauth-provider oauth-id]] identities]
      (-> criteria
          (dissoc :identities)
          (assoc [:identity :provider] oauth-provider
                 [:identity :provider-id] oauth-id)))
    criteria))

(defmethod sql/resolve-temp-ids :identity
  [ident id-map]
  (update-in ident [:user-id] id-map))
