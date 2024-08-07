(ns multi-money.db.sql.transactions
  (:require [multi-money.db.sql :as sql]))

(defn- inflate-item
  [trx-id {:as item :transaction-item/keys [debit-account credit-account]}]
  (-> item
      (assoc :transaction-item/transaction {:id trx-id}
             :transaction-item/debit-account-id (:id debit-account)
             :transaction-item/credit-account-id (:id credit-account))
      (dissoc :transaction-item/debit-account
              :transaction-item/credit-account)))

(defmethod sql/deconstruct :transaction
  [{:transaction/keys [items entity] :as transaction}]
  (let [id (or (:id transaction)
               (random-uuid))]
    (-> transaction
        (assoc :id id
               :transaction/entity-id (:id entity))
        (dissoc :transaction/items :transaction/entity)
        (cons (map (partial inflate-item id)
                   items)))))
