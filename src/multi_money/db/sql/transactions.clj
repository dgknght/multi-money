(ns multi-money.db.sql.transactions
  (:require [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]
            [multi-money.db.sql.types :refer [temp-id]]))

(defmethod sql/attributes :transaction [_]
  [:id :entity-id :date :description :memo])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs :transaction/entity)

(defmethod sql/deconstruct :transaction
  [{:transaction/keys [items date] :as transaction}]
  (let [id (or (:id transaction)
               (temp-id))
        with-id (assoc transaction :id id)]
    (-> with-id
        ->sql-refs
        (assoc :id id)
        (dissoc :transaction/items)
        (cons (map #(assoc %
                           :transaction-item/transaction with-id
                           :transaction-item/date date)
                   items)))))

(declare adj-model-refs)
(db/def->model-refs adj-model-refs :transaction/entity)

(defmethod sql/after-read :transaction
  [trx]
  (-> trx
      (update-in [:transaction/date] t/local-date)
      (adj-model-refs)))
