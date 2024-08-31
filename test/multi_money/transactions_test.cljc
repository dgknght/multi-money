(ns multi-money.transactions-test
  (:require [clojure.test :refer [deftest testing is]]
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            [multi-money.transactions :as trx]))

(def ^:private simple-unilateral
  #{#:transaction-item{:account {:id :checking}
                       :quantity 100M
                       :action :credit
                       :index 4
                       :balance 300M
                       :memo "notes about the purchase"}
    #:transaction-item{:account {:id :groceries}
                       :quantity 100M
                       :action :debit
                       :index 3
                       :balance 200M
                       :memo "notes about the purchase"}})

(def ^:private simple-bilateral
  #{#:transaction-item{:quantity 100M
                       :debit-account {:id :groceries}
                       :debit-index 3
                       :debit-balance 200M
                       :credit-account {:id :checking}
                       :credit-index 4
                       :credit-balance 300M
                       :memo "notes about the purchase"}})

(deftest convert-bilateral-to-unilateral
  (is (= simple-unilateral
         (trx/->unilateral simple-bilateral))
      "A single bilateral item is split into two unilateral items"))

(deftest convert-unilateral-to-bilateral
  (is (= simple-bilateral
         (trx/->bilateral simple-unilateral))
      "A single bilateral item is split into two unilateral items"))

(def ^:private transactions
  [#:transaction{:date "2020-01-01"
                 :description "Paycheck"
                 :items [#:transaction-item{:debit-account {:id :checking}
                                            :debit-index 0
                                            :debit-balance 5000M
                                            :credit-account {:id :salary}
                                            :credit-index 0
                                            :credit-balance 5000M
                                            :quantity 5000M}]}
   #:transaction{:date "2020-01-02"
                 :description "Landloard"
                 :items [#:transaction-item{:debit-account {:id :rent}
                                            :debit-index 0
                                            :debit-balance 2000M
                                            :credit-account {:id :checking}
                                            :credit-index 1
                                            :credit-balance 3000M
                                            :quantity 2000M}]}
   #:transaction{:date "2020-01-02"
                 :description "Kroger"
                 :items [#:transaction-item{:debit-account {:id :groceries}
                                            :debit-index 0
                                            :debit-balance 150M
                                            :credit-account {:id :credit-card}
                                            :credit-index 0
                                            :credit-balance 150M
                                            :quantity 150M}]}
   #:transaction{:date "2020-01-09"
                 :description "Kroger"
                 :items [#:transaction-item{:debit-account {:id :groceries}
                                            :debit-index 1
                                            :debit-balance 300M
                                            :credit-account {:id :credit-card}
                                            :credit-index 1
                                            :credit-balance 300M
                                            :quantity 150M}]}
   #:transaction{:date "2020-01-15"
                 :description "Paycheck"
                 :items [#:transaction-item{:debit-account {:id :checking}
                                            :debit-index 2
                                            :debit-balance 8000M
                                            :credit-account {:id :salary}
                                            :credit-index 1
                                            :credit-balance 10000M
                                            :quantity 5000M}]}
   #:transaction{:date "2020-01-16"
                 :description "Kroger"
                 :items [#:transaction-item{:debit-account {:id :groceries}
                                            :debit-index 2
                                            :debit-balance 450M
                                            :credit-account {:id :credit-card}
                                            :credit-index 2
                                            :credit-balance 450M
                                            :quantity 150M}]}
   #:transaction{:date "2020-01-23"
                 :description "Kroger"
                 :items [#:transaction-item{:debit-account {:id :groceries}
                                            :debit-index 3
                                            :debit-balance 600M
                                            :credit-account {:id :credit-card}
                                            :credit-index 3
                                            :credit-balance 600M
                                            :quantity 150M}]}
   #:transaction{:date "2020-01-30"
                 :description "Kroger"
                 :items [#:transaction-item{:debit-account {:id :groceries}
                                            :debit-index 4
                                            :debit-balance 750M
                                            :credit-account {:id :credit-card}
                                            :credit-index 4
                                            :credit-balance 750M
                                            :quantity 150M}]}
   #:transaction{:date "2020-01-31"
                 :description "Mastercard"
                 :items [#:transaction-item{:debit-account {:id :credit-card}
                                            :debit-index 5
                                            :debit-balance 150M
                                            :credit-account {:id :checking}
                                            :credit-index 3
                                            :credit-balance 7400M
                                            :quantity 600M}]}])

(defn- assert-item
  [items index quantity balance date]
  (let [item (nth items index)]
    (is (= index (trx/index item))
        "The index matches the item position in the list")
    (is (= quantity (trx/quantity item))
        "The item quantity is polarized")
    (is (= balance (trx/balance item))
        "The item has an appropriate balance")
    (is (= date (trx/date item))
        "The item reflects the transaction date")))

(deftest view-transactions-from-vis-a-vis-an-account
  (testing "The checking account"
    (let [checking (trx/per-account {:id :checking
                                     :account/type :asset}
                                    transactions)]
      (is (= 4 (count checking))
          "The checking items are returned")
      (assert-item checking 0  5000M 5000M "2020-01-01")
      (assert-item checking 1 -2000M 3000M "2020-01-02")
      (assert-item checking 2  5000M 8000M "2020-01-15")
      (assert-item checking 3  -600M 7400M "2020-01-31")))
  (testing "The credit card"
    (let [checking (trx/per-account {:id :credit-card
                                     :account/type :liability}
                                    transactions)]
      (is (= 6 (count checking))
          "The credit card items are returned")
      (assert-item checking 0  150M 150M "2020-01-02")
      (assert-item checking 1  150M 300M "2020-01-09")
      (assert-item checking 2  150M 450M "2020-01-16")
      (assert-item checking 3  150M 600M "2020-01-23")
      (assert-item checking 4  150M 750M "2020-01-30")
      (assert-item checking 5 -600M 150M "2020-01-31"))))

(deftest set-an-item-index
  (testing "The debit account"
    (let [item (->> transactions
                    (take 1)
                    (trx/per-account {:id :checking
                                      :account/type :asset})
                    first)]
      (is (= #:transaction{:date "2020-01-01"
                           :description "Paycheck"
                           :items [#:transaction-item{:debit-account {:id :checking}
                                                      :debit-index 101
                                                      :debit-balance 5000M
                                                      :credit-account {:id :salary}
                                                      :credit-index 0
                                                      :credit-balance 5000M
                                                      :quantity 5000M}]}
             (trx/transaction
               (trx/index item 101))))))
  (testing "The credit account"
    (let [item (->> transactions
                    (take 1)
                    (trx/per-account {:id :salary
                                      :account/type :income})
                    first)]
      (is (= #:transaction{:date "2020-01-01"
                           :description "Paycheck"
                           :items [#:transaction-item{:debit-account {:id :checking}
                                                      :debit-index 0
                                                      :debit-balance 5000M
                                                      :credit-account {:id :salary}
                                                      :credit-index 101
                                                      :credit-balance 5000M
                                                      :quantity 5000M}]}
             (trx/transaction
               (trx/index item 101)))))))

(deftest set-an-item-balance
  (testing "The debit account"
    (let [item (->> transactions
                    (take 1)
                    (trx/per-account {:id :checking
                                      :account/type :asset})
                    first)]
      (is (= #:transaction{:date "2020-01-01"
                           :description "Paycheck"
                           :items [#:transaction-item{:debit-account {:id :checking}
                                                      :debit-index 0
                                                      :debit-balance 1.01M
                                                      :credit-account {:id :salary}
                                                      :credit-index 0
                                                      :credit-balance 5000M
                                                      :quantity 5000M}]}
             (trx/transaction
               (trx/balance item 1.01M))))))
  (testing "The credit account"
    (let [item (->> transactions
                    (take 1)
                    (trx/per-account {:id :salary
                                      :account/type :income})
                    first)]
      (is (= #:transaction{:date "2020-01-01"
                           :description "Paycheck"
                           :items [#:transaction-item{:debit-account {:id :checking}
                                                      :debit-index 0
                                                      :debit-balance 5000M
                                                      :credit-account {:id :salary}
                                                      :credit-index 0
                                                      :credit-balance 1.01M
                                                      :quantity 5000M}]}
             (trx/transaction
               (trx/balance item 1.01M)))))))
