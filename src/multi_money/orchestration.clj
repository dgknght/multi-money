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
  [model date k]
  (update-in model [k] #(->> [% date]
                               (filter identity)
                               (sort t/before?)
                               first)))

(defn- update-last-trx-date
  [model date k]
  (update-in model
             [k]
             #(->> [% date]
                   (filter identity)
                   (sort t/after?)
                   first)))

(defn- affected-accounts
  [date items]
  (->> items
       (mapcat (juxt extract-debit
                     extract-credit))
       (map (comp #(update-1st-trx-date % date :account/first-transaction-date)
                  #(update-last-trx-date % date :account/last-transaction-date)
                  adj-act-balance))))

(defn- update-entity
  [entity date]
  (-> entity
      (update-1st-trx-date date :entity/first-transaction-date)
      (update-last-trx-date date :entity/last-transaction-date)))

(defn propagate-transaction
  [{:as trx :transaction/keys [items date entity]}]
  (cons trx
        (cons (update-entity entity date)
              (affected-accounts date items))))
