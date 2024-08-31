(ns multi-money.transactions
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.accounts :refer [polarize]]))

(defprotocol UnilateralItem
  (index [this] "Returns the index of the item vis-a-vis the account")
  (quantity [this] "Returns the quantity as a positive or negative number")
  (balance [this] "Returns the balance as a result of the item vis-a-vis the account")
  (date [this] "Returns the date of the transaction"))

(deftype CreditItem [trx-item trx account]
  UnilateralItem
  (index [_] (:transaction-item/credit-index trx-item))
  (quantity [_] (polarize (:transaction-item/quantity trx-item) :credit account))
  (balance [_] (:transaction-item/credit-balance trx-item))
  (date [_] (:transaction/date trx)))

(deftype DebitItem [trx-item trx account]
  UnilateralItem
  (index [_] (:transaction-item/debit-index trx-item))
  (quantity [_] (polarize (:transaction-item/quantity trx-item) :debit account))
  (balance [_] (:transaction-item/debit-balance trx-item))
  (date [_] (:transaction/date trx)))

(defn- =*
  [a b]
  (= (:id a) (:id b)))

(defn- lateralize
  [{:as item
    :transaction-item/keys [debit-account
                            credit-account]}
   trx
   account]
  (cond
    (=* debit-account account)
    (->DebitItem item trx account)
    
    (=* credit-account account)
    (->CreditItem item trx account)
    
    :else
    nil))

(defn per-account
  "Given an account and a list of transactions, returns a list of transaction
  items vis-a-vis the account."
  [account transactions]
  (->> transactions
       (mapcat (fn [trx]
                 (map #(lateralize % trx account)
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
