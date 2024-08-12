(ns multi-money.db.sql.transaction-items
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :transaction-item [_]
  [:id :date :transaction-id :debit-account-id :credit-account-id :quantity])

(defmethod sql/resolve-temp-ids :transaction-item
  [item id-map]
  (update-in item [:transaction-item/transaction-id] id-map))

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs
  :transaction-item/transaction
  :transaction-item/debit-account
  :transaction-item/credit-account)

(defmethod sql/before-save :transaction-item
  [item]
  (->sql-refs item))

(declare ->model-refs)
(db/def->model-refs ->model-refs
  :transaction-item/debit-account
  :transaction-item/credit-account
  :transaction-item/transaction)

(defmethod sql/after-read :transaction-item
  [item]
  (->model-refs item))
