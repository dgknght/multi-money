(ns multi-money.transactions
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.accounts :refer [polarize]]))

(defprotocol UnilateralItem
  (index [this] [this idx] "The 1 argument version returns the index, the 2 argument version sets the index and returns the new wrapper")
  (quantity [this] "Returns the quantity as a positive or negative number")
  (balance [this] "Returns the balance as a result of the item vis-a-vis the account")
  (date [this] "Returns the date of the transaction")
  (transaction [this] "Returns the underlying transaction"))

(declare ->CreditItem)
(declare ->DebitItem)

(deftype CreditItem [trx index account]
  UnilateralItem
  (index [_] (:transaction-item/credit-index (nth (:transaction/items trx) index)))
  (index [_ idx]
    (->CreditItem (assoc-in trx
                            [:transaction/items
                             index
                             :transaction-item/credit-index]
                            idx)
                  index
                  account))
  (quantity [_] (polarize (:transaction-item/quantity (nth (:transaction/items trx) index)) :credit account))
  (balance [_] (:transaction-item/credit-balance (nth (:transaction/items trx) index)))
  (date [_] (:transaction/date trx))
  (transaction [_] trx))

(deftype DebitItem [trx index account]
  UnilateralItem
  (index [_] (:transaction-item/debit-index (nth (:transaction/items trx) index)))
  (index [_ idx]
    (->DebitItem (assoc-in trx
                            [:transaction/items
                             index
                             :transaction-item/debit-index]
                            idx)
                  index
                  account))
  (quantity [_] (polarize (:transaction-item/quantity (nth (:transaction/items trx) index)) :debit account))
  (balance [_] (:transaction-item/debit-balance (nth (:transaction/items trx) index)))
  (date [_] (:transaction/date trx))
  (transaction [_] trx))

(defn- =*
  [a b]
  (= (:id a) (:id b)))

(defn- lateralize
  [trx
   index
   account]
  (let [{:transaction-item/keys
         [debit-account
          credit-account]} (nth (:transaction/items trx) index)]
    (cond
      (=* debit-account account)
      (->DebitItem trx index account)

      (=* credit-account account)
      (->CreditItem trx index account)

      :else
      nil)))

(defn per-account
  "Given an account and a list of transactions, returns a list of transaction
  items vis-a-vis the account."
  [account transactions]
  {:pre [(map? account)
         (:id account)
         (:account/type account)]}
  (->> transactions
       (mapcat (fn [trx]
                 (map-indexed (fn [idx _]
                                (lateralize trx idx account))
                              (:transaction/items trx))))
       (filter identity)))

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
