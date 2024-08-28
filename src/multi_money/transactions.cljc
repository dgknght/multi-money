(ns multi-money.transactions
  (:require [clojure.pprint :refer [pprint]]))

(defn- split-item
  [{:transaction-item/keys [debit-account
                            debit-index
                            debit-balance
                            credit-account
                            credit-index
                            credit-balance]
    :as item}]
  (let [stripped (dissoc item
                         :transaction-item/debit-account
                         :transaction-item/debit-index
                         :transaction-item/debit-balance
                         :transaction-item/credit-account
                         :transaction-item/credit-index
                         :transaction-item/credit-balance)]
    [(merge stripped
            #:transaction-item{:account debit-account
                               :index debit-index
                               :balance debit-balance
                               :action :debit})
     (merge stripped
            #:transaction-item{:account credit-account
                               :index credit-index
                               :balance credit-balance
                               :action :credit})]))

(defn ->unilateral
  [items]
  (->> items
       (mapcat split-item)
       (into #{})))

(defn- pair
  [{:keys [credit debit]}]
  (->> credit
       (interleave debit)
       (partition 2)
       (map (fn [[d c]]
              (-> c
                  (dissoc :transaction-item/action
                          :transaction-item/account
                          :transaction-item/index
                          :transaction-item/balance)
                  (assoc :transaction-item/debit-account (:transaction-item/account d)
                         :transaction-item/debit-index (:transaction-item/index d)
                         :transaction-item/debit-balance (:transaction-item/balance d)
                         :transaction-item/credit-account (:transaction-item/account c)
                         :transaction-item/credit-index (:transaction-item/index c)
                         :transaction-item/credit-balance (:transaction-item/balance c)))))))

(defn ->bilateral
  [items]
  (->> items
       (group-by :transaction-item/action)
       pair
       (into #{})))
