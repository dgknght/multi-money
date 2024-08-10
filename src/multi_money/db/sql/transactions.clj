(ns multi-money.db.sql.transactions
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [java-time.api :as t]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :transaction [_]
  [:id :entity-id :date :description :memo])

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
  [{:transaction/keys [items entity] :as transaction}]
  (let [id (or (:id transaction)
               (sql/temp-id))]
    (-> transaction
        (assoc :id id
               :transaction/entity-id (:id entity))
        (dissoc :transaction/items :transaction/entity)
        (cons (map (partial inflate-item (assoc transaction :id id))
                   items)))))

(defmethod sql/after-read :transaction
  [trx]
  (-> trx
      (update-in [:transaction/date] t/local-date)
      (update-in [:transaction/entity-id] db/->model-ref)
      (rename-keys {:transaction/entity-id :transaction/entity})))
