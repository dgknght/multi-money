(ns multi-money.models.transactions-test
  (:require [clojure.test :refer [is use-fixtures]]
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
            [multi-money.models.transactions :as trxs]
            [multi-money.db :as db]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            [multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(defn- attributes []
  (let [entity (find-entity "Personal")]
    #:transaction{:date (t/local-date 2020 3 2)
                  :description "Kroger"
                  :memo "notes about the purchase"
                  :entity entity
                  :items [{:debit-account (find-account "Credit Card" entity)
                           :credit-account (find-account "Dining" entity)
                           :quantity 100M}]}))

(dbtest create-a-transaction
  (with-context
    (let [attr (attributes)
          result (trxs/put attr)]
      (is (comparable? attr result)
          "The result contains the correct attributes")
      (is (comparable? attr (trxs/find result))
          "The transaction can be retrieved")
      (is (:id result)
          "The result contains an :id value"))))

(dbtest transaction-date-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:date ["date is required"]}}
          (trxs/put (dissoc (attributes)
                            :transaction/date))))))

(dbtest transaction-description-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:description ["description is required"]}}
          (trxs/put (dissoc (attributes)
                            :transaction/description))))))

(dbtest transaction-items-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["items is required"]}}
          (trxs/put (dissoc (attributes)
                            :transaction/items))))))

(dbtest transaction-must-contain-at-least-one-item
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["Must contain at least one item"]}}
          (trxs/put (assoc (attributes)
                            :transaction/items []))))))

(dbtest transaction-item-accounts-cannot-have-different-entities
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["All items must belong to the same entity"]}}
          (trxs/put (assoc (attributes)
                            :transaction/items [{:debit-account (find-account "Checking" "Business")
                                                 :credit-account (find-account "Rent" "Personal")
                                                 :quantity 100M}]))))))

(dbtest transaction-item-accounts-must-have-the-same-entity-as-the-transaction
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:transaction{:items ["All items must belong to the same entity"]}}
          (trxs/put (assoc (attributes)
                            :transaction/entity (find-entity "Business")))))))

(def ^:private existing-trxs
  (assoc basic-context
         :transactions
         [#:transaction{:description "Paycheck"
                        :date (t/local-date 2020 1 1)
                        :items [#:transaction-item{:debit-account (find-account "Checking" "Personal")
                                                   :credit-account (find-account "Salary" "Personal")
                                                   :quantity 5000M}]}
          #:transaction{:description "Landlord"
                        :date (t/local-date 2020 1 2)
                        :items [#:transaction-item{:debit-account (find-account "Rent" "Personal")
                                                   :credit-account (find-account "Checking" "Personal")
                                                   :quantity 1000M}]}
          #:transaction{:description "Kroger"
                        :date (t/local-date 2020 1 3)
                        :items [#:transaction-item{:debit-account (find-account "Groceries" "Personal")
                                                   :credit-account (find-account "Credit Card" "Personal")
                                                   :quantity 100M}]}]))

(dbtest update-a-transaction
  (with-context existing-trxs
    (let [transaction (find-transaction (t/local-date 2020 1 1) "Landlord")
          updated (trxs/put (assoc-in transaction
                                      [:transaction/items 0 :transaction-item/amount] 1001M))]
      (is (comparable? #:transaction{:name "Cheques"}
                       updated)
          "The result contains the updated attributes")
      (is (comparable? #:transaction{:name "Cheques"}
                       (trxs/find transaction))
          "A retrieved model has the updated attributes"))))

(dbtest fetch-all-transactions-for-an-account
  (with-context existing-trxs
    (is (seq-of-maps-like? [#:transaction{:date (t/local-date 2020 1 1) :description "Paycheck"}
                            #:transaction{:date (t/local-date 2020 1 2) :description "Landlord"}]
                           (trxs/select {:transaction/account (db/->model-ref
                                                                (find-account "Checking"))})))))

(dbtest delete-a-transaction
  (with-context existing-trxs
    (let [transaction (find-transaction (t/local-date 2020 1 3) "Kroger")]
      (trxs/delete transaction)
      (is (nil? (trxs/find transaction))
          "The transaction cannot be retrieved after delete"))))

(dbtest get-a-count-of-transactions
  (with-context existing-trxs
    (is (= 5 (trxs/count {:transaction/entity (find-entity "Personal")})))))
