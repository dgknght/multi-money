(ns multi-money.models.accounts-test
  (:require [clojure.test :refer [is use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [multi-money.helpers :refer [reset-db
                                        dbtest]]
            [multi-money.test-context :refer [with-context
                                              find-entity
                                              find-account
                                              find-commodity]]
            [multi-money.models.accounts :as acts]
            [multi-money.db :as db]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            [multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(defn- attributes
  [& [entity commodity]]
  #:account{:name "Savings"
            :entity (select-keys (or entity
                                     (find-entity "Personal"))
                                 [:id])
            :commodity (select-keys (or commodity
                                        (find-commodity "USD" "Personal"))
                                    [:id])
            :type :asset})

(dbtest create-an-account
  (with-context
    (let [entity (find-entity "Personal")
          attr (attributes entity)
          result (acts/put attr)]
      (is (comparable? attr result)
          "The result contains the correct attributes")
      (is (comparable? attr (acts/find result))
          "The account can be retrieved")
      (is (:id result)
          "The result contains an :id value"))))

(dbtest account-name-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:account{:name ["Name is required"]}}
          (acts/put (dissoc (attributes)
                            :account/name))))))

(dbtest account-type-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:account{:type ["Type is required"]}}
          (acts/put (dissoc (attributes)
                            :account/type))))))

(dbtest account-type-must-be-asset-or-liability-or-income-or-expense-or-equity
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:account{:type ["Type must be expense, equity, liability, income, or asset"]}}
          (acts/put (assoc (attributes)
                           :account/type :unknown))))))

(dbtest account-entity-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:account{:entity ["Entity is required"]}}
          (acts/put (dissoc (attributes)
                            :account/entity))))))

(dbtest account-commodity-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:account{:commodity ["Commodity is required"]}}
          (acts/put (dissoc (attributes)
                            :account/commodity))))))

(dbtest account-name-is-unique-for-an-entity
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:account{:name ["Name is already in use"]}}
          (acts/put (assoc (attributes)
                           :account/name "Checking"))))))

(dbtest account-name-is-not-unique-across-entities
  (with-context
    (let [entity (find-entity "Business")
          result (acts/put (attributes entity))]
      (is (valid? result) "The result does not indicate any validation errors")
      (is (:id result)
          "An :id is assigned"))))

(dbtest update-an-account
  (with-context
    (let [account (find-account "Checking")
          updated (acts/put (assoc account
                                   :account/name "Cheques"))]
      (is (comparable? #:account{:name "Cheques"}
                       updated)
          "The result contains the updated attributes")
      (is (comparable? #:account{:name "Cheques"}
                       (acts/find account))
          "A retrieved model has the updated attributes"))))

(dbtest cannot-update-to-an-existing-account-name-in-same-entity
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:account/name ["Name is already in use"]}}
          (-> (find-account "Groceries")
              (assoc :account/name "Rent")
              acts/put)))))

(dbtest fetch-all-accounts-for-an-entity
        (with-context
          (is (seq-of-maps-like? [#:account{:name "Checking"}
                                  #:account{:name "Credit Card"}
                                  #:account{:name "Salary"}
                                  #:account{:name "Rent"}
                                  #:account{:name "Groceries"}]
                                 (acts/select {:account/entity (db/->model-ref
                                                                 (find-entity "Personal"))})))))

(dbtest delete-an-account
  (with-context
    (let [account (find-account "Checking")]
      (acts/delete account)
      (is (nil? (acts/find account))
          "The account cannot be retrieved after delete"))))

(dbtest get-a-count-of-accounts
  (with-context
    (is (= 5 (acts/count {:account/entity (find-entity "Personal")})))))
