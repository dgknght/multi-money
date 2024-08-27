(ns multi-money.transactions)

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
       (mapcat split-item)))
