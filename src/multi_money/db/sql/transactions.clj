(ns multi-money.db.sql.transactions
  (:require [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [stowaway.criteria :as crt]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]
            [multi-money.db.sql.types :refer [temp-id]]))

(defmethod sql/attributes :transaction [_]
  [:id :entity-id :date :description :memo])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs :transaction/entity)

(declare ->item-sql-refs)
(sql/def->sql-refs ->item-sql-refs
  :transaction-item/debit-account
  :transaction-item/credit-account)

(defmethod sql/before-save :transaction
  [trx]
  (->sql-refs trx))

(defmethod sql/deconstruct :transaction
  [{:transaction/keys [items date] :as transaction}]
  (let [id (or (:id transaction)
               (temp-id))
        with-id (assoc transaction :id id)]
    (-> with-id
        (dissoc :transaction/items)
        (cons (mapv #(assoc %
                            :transaction-item/transaction with-id
                            :transaction-item/date date)
                    items)))))

(declare ->model-refs)
(db/def->model-refs ->model-refs :transaction/entity)

(defmethod sql/after-read :transaction
  [trx]
  (-> trx
      (update-in [:transaction/date] t/local-date)
      (->model-refs)))

(defmethod sql/prepare-criteria :transaction
  [criteria]
  (crt/apply-to criteria (comp ->sql-refs ->item-sql-refs)))
