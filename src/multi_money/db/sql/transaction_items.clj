(ns multi-money.db.sql.transaction-items
  (:require [multi-money.db.sql :as sql]))

(defmethod sql/attributes :transaction-item [_]
  [:id :date :transaction-id :debit-account-id :credit-account-id :quantity])

(defmethod sql/resolve-temp-ids :transaction-item
  [item id-map]
  (update-in item [:transaction-item/transaction-id] id-map))
