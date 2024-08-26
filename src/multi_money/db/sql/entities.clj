(ns multi-money.db.sql.entities
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.criteria :as crt]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.dates :refer [->local-date]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :entity [_]
  [:id
   :name
   :owner-id
   :default-commodity-id
   :first-transaction-date
   :last-transaction-date])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs :entity/owner :entity/default-commodity)

(defmethod sql/before-save :entity
  [entity]
  (->sql-refs entity))

(declare ->model-refs)
(db/def->model-refs ->model-refs :entity/owner :entity/default-commodity)

(defmethod sql/after-read :entity
  [entity]
  (-> entity
      ->model-refs
      (update-in-if [:entity/first-transaction-date] ->local-date)
      (update-in-if [:entity/last-transaction-date] ->local-date)))

(defmethod sql/prepare-criteria :entity
  [criteria]
  (crt/apply-to criteria ->sql-refs))
