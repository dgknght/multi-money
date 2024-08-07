(ns multi-money.db.sql.users
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [update-in-criteria]]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :user [_]
  [:id :username :email :given-name :surname])

(defn- inflate-identity
  [user-id [p id]]
  #:identity{:user-id user-id
             :oauth-provider p
             :oauth-id id})

(defmethod sql/deconstruct :user
  [{:as user :user/keys [identities]}]
  (let [id (or (:id user) (random-uuid))]
    (-> user
        (assoc :id id)
        (dissoc :user/identities)
        (cons (map (partial inflate-identity id)
                   identities)))))

(defmethod sql/resolve-temp-ids :identity
  [ident id-map]
  (update-in ident [:identity/user-id] id-map))

(defmethod sql/prepare-criteria :user
 [criteria]
 (update-in-criteria criteria [:identity/oauth-provider] name))
