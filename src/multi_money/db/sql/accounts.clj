(ns multi-money.db.sql.accounts
  (:require [stowaway.criteria :as crt]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.dates :refer [->local-date]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :account [_]
  [:id
   :name
   :entity-id
   :commodity-id
   :parent-id
   :type
   :quantity
   :first-transaction-date
   :last-transaction-date])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs
  :account/entity
  :account/commodity
  :account/parent)

(defmethod sql/prepare-criteria :account
  [criteria]
  (crt/apply-to criteria ->sql-refs))

(defmethod sql/before-save :account
  [account]
  (-> account
      (update-in [:account/type] name)
      ->sql-refs))

(declare ->model-refs)
(db/def->model-refs ->model-refs
  :account/entity
  :account/commodity
  :account/parent)

(defmethod sql/after-read :account
  [account]
  (-> account
      ->model-refs
      (update-in [:account/type] keyword)
      (update-in-if [:account/first-transaction-date] ->local-date)
      (update-in-if [:account/last-transaction-date] ->local-date)))
