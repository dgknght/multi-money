(ns multi-money.db.sql.queries
  (:refer-clojure :exclude [format])
  (:require [clojure.pprint :refer [pprint]]
            [honey.sql :refer [format]]
            [honey.sql.helpers :refer [select
                                       from]]
            [stowaway.sql :refer [apply-criteria]]
            [dgknght.app-lib.inflection :refer [plural]]
            [multi-money.util :refer [update-in-criteria
                                      qualifier]]
            [multi-money.db :as db]
            [multi-money.db.sql.types :refer [coerce-id]]))

(derive clojure.lang.PersistentArrayMap ::map)
(derive clojure.lang.PersistentVector ::vector)

(def infer-table-name
  (comp keyword
        plural
        qualifier))

(defn- apply-options
  [s {:keys [limit order-by count]}]
  (if count
    (assoc s :select [[:%count.1 :record-count]])
    (cond-> s
      limit (assoc :limit limit)
      order-by (assoc :order-by order-by))))

(def ^:private query-options
  {:relationships {#{:user :identity} {:primary-table :users
                                       :foreign-table :identities
                                       :foreign-id :user_id}
                   #{:user :entity}   {:primary-table :users
                                       :foreign-table :entities
                                       :foreign-id :owner_id}
                   #{:entity :commodity} {:primary-table :entities
                                          :foreign-table :commodities
                                          :foreign-id :entity_id}}})

(defn criteria->query
  [criteria & [options]]
  {:pre [criteria]}

  (if-let [model-type (db/model-type criteria)]
    (-> (select :*)
        (from (keyword (plural model-type)))
        (apply-criteria (update-in-criteria criteria [:id] coerce-id)
                        (merge query-options
                               {:target (keyword model-type)}))
        (apply-options options)
        format)
    (throw (IllegalArgumentException. "Unable to determine the model type from the criteria."))))
