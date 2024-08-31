(ns multi-money.models.transactions-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [multi-money.helpers :refer [reset-db
                                        dbtest]]
            [multi-money.test-context :refer [with-context
                                              basic-context
                                              find-entity
                                              find-account
                                              find-transaction]]
            [multi-money.models.entities :as ents]
            [multi-money.models.accounts :as acts]
            [multi-money.models.transactions :as trxs]
            [multi-money.db :as db]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            [multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(defn- attributes
  ([] (attributes (find-entity "Personal")))
  ([entity]
   #:transaction{:date (t/local-date 2020 3 2)
                 :description "Kroger"
                 :memo "notes about the purchase"
                 :entity (db/->model-ref entity)
                 :items [#:transaction-item{:debit-account (db/->model-ref (find-account ["Groceries" entity]))
                                            :credit-account (db/->model-ref (find-account ["Credit Card" entity]))
                                            :quantity 100M}]}))

(dbtest create-a-transaction
  (with-context
    (let [entity (find-entity "Personal")
          attr (attributes entity)
          result (trxs/put attr)]
      (testing "the result"
        (is (comparable? attr result)
            "The result contains the correct attributes")
        (is (:id result)
            "The result contains an :id value")
        (is (seq-of-maps-like? [#:transaction-item{:debit-index 1
                                                   :debit-balance 100M
                                                   :credit-index 1
                                                   :credit-balance 100M}]
                               (:transaction/items result))
            "The transaction item receives denormalization attributes"))
      #_(testing "retrievability"
        (is (comparable? attr (trxs/find result))
            "The transaction can be retrieved"))
      #_(testing "entity updates"
        (let [{:entity/keys [first-transaction-date
                             last-transaction-date]} (ents/find entity)]
          (is (= (t/local-date 2020 3 2)
                 first-transaction-date)
              ":first-transaction-date is set on the entity")
          (is (= (t/local-date 2020 3 2)
                 last-transaction-date)
              ":last-transaction-date is set on the entity")))
      #_(testing "debit account updates"
        (let [{:account/keys
               [first-transaction-date
                last-transaction-date
                balance]} (acts/find (get-in result
                                             [:transaction/items
                                              0
                                              :transaction-item/debit-account]))]
          (is (= (t/local-date 2020 3 2)
                 first-transaction-date)
              ":first-transaction-date is set on the debit account")
          (is (= (t/local-date 2020 3 2)
                 last-transaction-date)
              ":last-transaction-date is set on the debit account")
          (is (= 100M balance)
              "The :quantity is set on the debit account")))
      #_(testing "credit account updates"
        (let [{:account/keys
               [first-transaction-date
                last-transaction-date
                balance]} (acts/find (get-in result
                                             [:transaction/items
                                              0
                                              :transaction-item/credit-account]))]
          (is (= (t/local-date 2020 3 2)
                 first-transaction-date)
              ":first-transaction-date is set on the credit account")
          (is (= (t/local-date 2020 3 2)
                 last-transaction-date)
              ":last-transaction-date is set on the credit account")
          (is (= 100M balance)
              "The :quantity is set on the credit account"))))))

(dbtest transaction-date-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:date ["Date is required"]}}
          (trxs/put (dissoc (attributes)
                            :transaction/date))))))

(dbtest transaction-description-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:description ["Description is required"]}}
          (trxs/put (dissoc (attributes)
                            :transaction/description))))))

(dbtest transaction-items-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["Items is required"]}}
          (trxs/put (dissoc (attributes)
                            :transaction/items))))))

(dbtest transaction-must-contain-at-least-one-item
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["Items must contain at least 1 item(s)"]}}
          (trxs/put (assoc (attributes)
                            :transaction/items []))))))

(def ^:private business-accounts-ctx
  (-> basic-context
      (update-in [:commodities] conj #:commodity{:name "US Dollar"
                                                 :symbol "USD"
                                                 :type :currency
                                                 :entity "Business"})
      (update-in [:accounts] conj #:account{:name "Checking"
                                            :type :asset
                                            :entity "Business"})))

(dbtest transaction-item-accounts-cannot-have-different-entities
  (with-context business-accounts-ctx
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["All items must have accounts that belong to the same entity as the transaction"]}}
          (trxs/put (assoc (attributes)
                           :transaction/items [#:transaction-item{:debit-account (find-account ["Checking" "Business"])
                                                                  :credit-account (find-account ["Rent" "Personal"])
                                                                  :quantity 100M}]))))))

(dbtest transaction-item-accounts-must-have-the-same-entity-as-the-transaction
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["All items must have accounts that belong to the same entity as the transaction"]}}
          (trxs/put (assoc (attributes)
                            :transaction/entity (find-entity "Business")))))))

(def ^:private existing-trxs
  (assoc basic-context
         :transactions
         [#:transaction{:description "Paycheck"
                        :date (t/local-date 2020 1 1)
                        :items [#:transaction-item{:debit-account ["Checking" "Personal"]
                                                   :credit-account ["Salary" "Personal"]
                                                   :quantity 5000M}]}
          #:transaction{:description "Landlord"
                        :date (t/local-date 2020 1 2)
                        :items [#:transaction-item{:debit-account ["Rent" "Personal"]
                                                   :credit-account ["Checking" "Personal"]
                                                   :quantity 1000M}]}
          #:transaction{:description "Kroger"
                        :date (t/local-date 2020 1 3)
                        :items [#:transaction-item{:debit-account ["Groceries" "Personal"]
                                                   :credit-account ["Credit Card" "Personal"]
                                                   :quantity 100M}]}]))

(dbtest create-a-transaction-after-an-existing-one
  (with-context existing-trxs
    (let [groceries (find-account ["Groceries" "Personal"])
          checking (find-account ["Checking" "Personal"])
          {:transaction/keys [items]}
          (trxs/put
            #:transaction{:description "Market Street"
                          :date (t/local-date 2020 1 4)
                          :entity (:account/entity groceries)
                          :items [#:transaction-item{:debit-account groceries
                                                     :credit-account checking
                                                     :quantity 25M}]})]
      (is (seq-of-maps-like? [#:transaction-item{:debit-index 2
                                                 :debit-balance 125M
                                                 :credit-index 2
                                                 :credit-balance 3075M}]
                             items)
          "The transaction item receives denormalization attributes")
      (is (= 125M (:account/balance (acts/find groceries)))
          "The credit account balance is updated")
      (is (= 3075M (:account/balance (acts/find checking)))
          "The debit account balance is updated"))))
; TODO: Test the transaction items for each account

(dbtest update-a-transaction
  (with-context existing-trxs
    (let [transaction (find-transaction [(t/local-date 2020 1 2) "Landlord"])
          updated (trxs/put (assoc transaction :transaction/description "Landdude"))]
      (is (comparable? #:transaction{:description "Landdude"}
                       updated)
          "The result contains the updated attributes")
      (is (comparable? #:transaction{:description "Landdude"}
                       (trxs/find transaction))
          "A retrieved model has the updated attributes"))))

(dbtest fetch-all-transactions-for-an-account
  (with-context existing-trxs
    (is (seq-of-maps-like? [#:transaction{:date (t/local-date 2020 1 1) :description "Paycheck"}
                            #:transaction{:date (t/local-date 2020 1 2) :description "Landlord"}]
                           (trxs/select {:transaction/account (find-account "Checking")
                                         :transaction/date [:between
                                                            (t/local-date 2020 1 1)
                                                            (t/local-date 2020 1 3)]})))))

(deftest transaction-criteria-must-include-a-date-range
  (is (thrown? java.lang.AssertionError
               (trxs/select {:transaction/entity {:id 1}}))
      "No date attributes is invalid"))

(dbtest delete-a-transaction
  (with-context existing-trxs
    (let [transaction (find-transaction [(t/local-date 2020 1 3) "Kroger"])]
      (trxs/delete transaction)
      (is (nil? (trxs/find transaction))
          "The transaction cannot be retrieved after delete"))))

(dbtest get-a-count-of-transactions
  (with-context existing-trxs
    (is (= 3 (trxs/count {:transaction/entity (find-entity "Personal")
                          :transaction/date [:between>
                                             (t/local-date 2020 1 1)
                                             (t/local-date 2021 1 1)]})))))
