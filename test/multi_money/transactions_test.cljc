(ns multi-money.transactions-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.transactions :as trxs]))

(def ^:private simple-unilateral
  #{#:transaction-item{:account :checking
                       :quantity 100M
                       :action :credit
                       :index 4
                       :balance 300M
                       :memo "notes about the purchase"}
    #:transaction-item{:account :groceries
                       :quantity 100M
                       :action :debit
                       :index 3
                       :balance 200M
                       :memo "notes about the purchase"}})

(def ^:private simple-bilateral
  #{#:transaction-item{:quantity 100M
                       :debit-account :groceries
                       :debit-index 3
                       :debit-balance 200M
                       :credit-account :checking
                       :credit-index 4
                       :credit-balance 300M
                       :memo "notes about the purchase"}})

(deftest convert-bilateral-to-unilateral
  (is (= simple-unilateral
         (trxs/->unilateral simple-bilateral))
      "A single bilateral item is split into two unilateral items"))

(deftest convert-unilateral-to-bilateral
  (is (= simple-bilateral
         (trxs/->bilateral simple-unilateral))
      "A single bilateral item is split into two unilateral items"))

