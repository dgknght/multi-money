(ns multi-money.db.sql.queries
  (:refer-clojure :exclude [format])
  (:require [clojure.pprint :refer [pprint]]
            [honey.sql :refer [format]]
            [honey.sql.helpers :refer [select
                                       from]]
            [stowaway.sql :refer [apply-criteria]]
            [dgknght.app-lib.inflection :refer [plural]]
            [multi-money.db :as db]
            [multi-money.util :refer [update-in-criteria]]
            [multi-money.db.sql.types :refer [coerce-id]]))

(derive clojure.lang.PersistentArrayMap ::map)
(derive clojure.lang.PersistentVector ::vector)

(def infer-table-name
  (comp plural
        db/model-type))

(defn- apply-options
  [s {:keys [limit order-by]}]
  (cond-> s
    limit (assoc :limit limit)
    order-by (assoc :order-by order-by)))

(def ^:private query-options
  {:relationships {#{:user :identity} {:primary-table :users
                                       :foreign-table :identities
                                       :foreign-id :user_id}}})

(defn criteria->query
  [criteria & [options]]
  {:pre [(db/model-type criteria)]}

  (-> (select :*)
      (from (infer-table-name criteria))
      (apply-criteria (update-in-criteria criteria [:id] coerce-id)
                      (merge query-options
                             {:target (db/model-type criteria)}))
      (apply-options options)
      format))