(ns multi-money.orchestration-test
  (:require [clojure.test :refer [is use-fixtures]]
            [java-time.api :as t]
            [multi-money.helpers :refer [reset-db
                                         dbtest]]
            [multi-money.test-context :refer [with-context
                                              find-entity
                                              find-account]]
            [multi-money.db :as db]
            [multi-money.models.entities :as ents]
            [multi-money.models.accounts :as acts]
            [multi-money.models.transactions :as trxs]
            [multi-money.orchestration :as orc]))

(use-fixtures :each reset-db)

(defn- attributes
  ([] (attributes (find-entity "Personal")))
  ([entity]
   #:transaction{:date (t/local-date 2020 3 2)
                 :description "Kroger"
                 :memo "notes about the purchase"
                 :entity (db/->model-ref entity)
                 :items [#:transaction-item{:debit-account (db/->model-ref (find-account ["Credit Card" entity]))
                                            :credit-account (db/->model-ref (find-account ["Groceries" entity]))
                                            :quantity 100M}]}))
(dbtest create-a-transaction
  (with-context
    (let [entity (find-entity "Personal")
          attr (attributes entity)
          {:keys [transaction entity accounts]} (orc/create-transaction attr)]
      (is (comparable? attr result)
          "The result contains the correct attributes")
      (is (comparable? attr (trxs/find result))
          "The transaction can be retrieved")
      (is (:id result)
          "The result contains an :id value")
      (let [entity (ents/find entity) 
            groceries (acts/find (find-account "Groceries"))
            credit-card (acts/find (find-account "Credit Card"))]
        (is (= (t/local-date 2020 3 2)
               (:entity/first-transaction-date entity))
            "The entities :first-transaction-date attributes is updated")
        (is (= (t/local-date 2020 3 2)
               (:entity/last-transaction-date entity))
            "The entities :last-transaction-date attributes is updated")
        (is (= (t/local-date 2020 3 2)
               (:account/first-transaction-date groceries))
            "The :first-transaction-date attributes is updated for the debit account")
        (is (= (t/local-date 2020 3 2)
               (:account/last-transaction-date groceries))
            "The :last-transaction-date attributes is updated for the debit account")
        (is (= (t/local-date 2020 3 2)
               (:account/first-transaction-date credit-card))
            "The :first-transaction-date attributes is updated for the debit account")
        (is (= (t/local-date 2020 3 2)
               (:account/last-transaction-date credit-card))
            "The :last-transaction-date attributes is updated for the debit account")))))
