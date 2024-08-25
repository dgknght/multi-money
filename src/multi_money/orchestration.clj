(ns multi-money.orchestration
  (:require [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [multi-money.accounts :refer [polarize]]
            [multi-money.models.accounts :as acts]))

(defn- resolve-account
  [account]
  (if (:account/name account)
    account
    (acts/find account)))

(def ^:private extract-debit
  (juxt (constantly :debit)
        (comp resolve-account
              :transaction-item/debit-account)
        :transaction-item/quantity))

(def ^:private extract-credit
  (juxt (constantly :credit)
        (comp resolve-account
              :transaction-item/credit-account)
        :transaction-item/quantity))

(defn- adj-act-balance
  [[action account quantity]]
  (update-in account [:account/quantity] + (polarize quantity action account)))

(defn- update-1st-trx-date
  [account date]
  (update-in account
             [:account/first-transaction-date]
             #(->> [% date]
                   (filter identity)
                   (sort t/before?)
                   first)))

(defn- update-last-trx-date
  [account date]
  (update-in account
             [:account/last-transaction-date]
             #(->> [% date]
                   (filter identity)
                   (sort t/after?)
                   first)))

(defn- affected-accounts
  [date items]
  (->> items
       (mapcat (juxt extract-debit
                     extract-credit))
       (map (comp #(update-1st-trx-date % date)
                  #(update-last-trx-date % date)
                  adj-act-balance))))

(defn propagate-transaction
  [{:as trx :transaction/keys [items date]}]
  (cons trx (affected-accounts date items)))
