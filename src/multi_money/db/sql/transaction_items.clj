(ns multi-money.db.sql.transaction-items
  (:require [clojure.set :refer [rename-keys]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :transaction-item [_]
  [:id :date :transaction-id :debit-account-id :credit-account-id :quantity])

(defmethod sql/resolve-temp-ids :transaction-item
  [item id-map]
  (update-in item [:transaction-item/transaction-id] id-map))

(defmethod sql/after-read :transaction-item
  [item]
  (-> item
      (rename-keys {:transaction-item/debit-account-id :transaction-item/debit-account
                    :transaction-item/credit-account-id :transaction-item/credit-account
                    :transaction-item/transaction-id :transaction-item/transaction})
      (update-in [:transaction-item/debit-account] db/->model-ref)
      (update-in [:transaction-item/credit-account] db/->model-ref)
      (update-in [:transaction-item/transaction] db/->model-ref)))
