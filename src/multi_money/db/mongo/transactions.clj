(ns multi-money.db.mongo.transactions
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :as utl]
            [multi-money.db.mongo :as m]))

(declare ->trx-mongo-refs)
(m/def->mongo-refs ->trx-mongo-refs
  :transaction/entity)

(declare ->item-mongo-refs)
(m/def->mongo-refs ->item-mongo-refs
  :transaction-item/debit-account
  :transaction-item/credit-account)

(defn- before-save-items
  [items]
  (map ->item-mongo-refs items))

(defmethod m/before-save :transaction
  [transaction]
  (-> transaction
      ->trx-mongo-refs
      (update-in [:transaction/items] before-save-items)))

(declare <-trx-mongo-refs)
(m/def<-mongo-refs <-trx-mongo-refs
  :transaction/entity)

(declare <-item-mongo-refs)
(m/def<-mongo-refs <-item-mongo-refs
  :transaction-item/debit-account
  :transaction-item/credit-account)

(defn- after-read-items
  [items]
  (map (comp <-item-mongo-refs
             #(utl/qualify-keys % :transaction-item)) items))

(defmethod m/after-read :transaction
  [transaction]
  (-> transaction
      <-trx-mongo-refs
      (update-in [:transaction/items] after-read-items)))

(defmethod m/prepare-criteria :transaction
  [criteria]
  (->trx-mongo-refs criteria))
