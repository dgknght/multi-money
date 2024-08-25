(ns multi-money.orchestration
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.models.accounts :as acts]))

(defn- affected-accounts
  [items]
  (let [x (->> items
               (mapcat (juxt (juxt :transaction-item/debit-account
                                   :transaction-item/quantity)
                             (juxt :transaction-item/credit-account
                                   :transaction-item/quantity))))]
    (pprint {::affected-accounts x}))
  [])

(defn propagate-transaction
  [{:as trx :transaction/keys [items]}]
  (cons trx (affected-accounts items)))
