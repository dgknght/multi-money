(ns multi-money.db.datomic.transactions
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.criteria :as crt]
            [multi-money.dates :refer [->java-date
                                       ->local-date]]
            [multi-money.db.datomic :as d]))

(declare ->trx-ids)
(d/def->ids ->trx-ids :transaction/entity)

(declare ->item-ids)
(d/def->ids ->item-ids
  :transaction-item/debit-account
  :transaction-item/credit-account)

(defmethod d/before-save :transaction
  [trx]
  (-> trx
      ->trx-ids
      (update-in [:transaction/date] ->java-date)
      (update-in [:transaction/items] #(map ->item-ids %))))

(defmethod d/prepare-criteria :transaction
  [criteria]
  (crt/apply-to criteria (comp ->trx-ids
                               ->item-ids)))

(defmethod d/after-read :transaction
  [trx]
  (update-in trx [:transaction/date] ->local-date))
