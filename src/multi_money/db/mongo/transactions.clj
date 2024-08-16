(ns multi-money.db.mongo.transactions
  (:require [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [prewalk]]
            [java-time.api :as t]
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

(defn- coerce-dates
  [criteria]
  (prewalk #(if (t/local-date? %)
              (t/java-date
                (t/zoned-date-time %
                                   (t/local-time 0 0 0 0)
                                   (t/zone-offset 0 0)))
              %)
           criteria))

(defmethod m/prepare-criteria :transaction
  [criteria]
  (-> criteria
      ->trx-mongo-refs
      coerce-dates))
