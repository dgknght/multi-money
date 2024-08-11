(ns multi-money.db.sql.transactions
  (:require [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :transaction [_]
  [:id :entity-id :date :description :memo])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs :transaction/entity)

(defn- inflate-item
  [trx {:as item :transaction-item/keys [debit-account credit-account]}]
  (-> item
      (assoc :transaction-item/date (:transaction/date trx)
             :transaction-item/transaction-id (:id trx)
             :transaction-item/debit-account-id (:id debit-account)
             :transaction-item/credit-account-id (:id credit-account))
      (dissoc :transaction-item/debit-account
              :transaction-item/credit-account)))

(defmethod sql/deconstruct :transaction
  [{:transaction/keys [items] :as transaction}]
  (let [id (or (:id transaction)
               (sql/temp-id))
        with-id (assoc transaction :id id)]
    (-> with-id
        ->sql-refs
        (assoc :id id)
        (dissoc :transaction/items)
        (cons (map (partial inflate-item with-id)
                   items)))))

(declare adj-model-refs)
(db/def->model-refs adj-model-refs :transaction/entity)

(defmethod sql/after-read :transaction
  [trx]
  (-> trx
      (update-in [:transaction/date] t/local-date)
      (adj-model-refs)))
