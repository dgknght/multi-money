(ns multi-money.db.sql.accounts
  (:require [multi-money.util :refer [apply-to-criteria]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :account [_]
  [:id :name :entity-id :commodity-id :parent-id :type])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs
  :account/entity
  :account/commodity
  :account/parent)

(defmethod sql/prepare-criteria :account
  [criteria]
  (apply-to-criteria criteria ->sql-refs))

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
      (update-in [:account/type] keyword)))
