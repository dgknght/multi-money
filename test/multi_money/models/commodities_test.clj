(ns multi-money.models.commodities-test
  (:require [clojure.test :refer [is use-fixtures]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id]]
            [multi-money.test-context :refer [with-context
                                              basic-context
                                              find-commodity
                                              find-entity]]
            [multi-money.helpers :refer [reset-db
                                        dbtest]]
            [multi-money.models.commodities :as cdts]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            [multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(defn- attributes
  ([] (attributes (find-entity "Personal")))
  ([entity]
   #:commodity{:entity entity
               :name "Canadian Dollar"
               :symbol "CAD"
               :type :currency}))

(dbtest create-a-commodity
        (with-context
          (let [attr (attributes)
                result (cdts/put attr)
                expected (update-in attr [:commodity/entity] ->id)]
            (is (:id result) "The result contains an :id")
            (is (comparable? expected result)
                "The result contains the specified attributes")
            (is (comparable? expected (cdts/find (:id result)))
                "The commodity can be retrieved by :id"))))

(dbtest entity-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:commodity/entity ["Entity is required"]}}
          (cdts/put (dissoc (attributes) :commodity/entity))))))

(dbtest symbol-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:commodity/symbol ["Symbol is required"]}}
          (cdts/put (dissoc (attributes) :commodity/symbol))))))

(dbtest symbol-is-unique-for-an-entity
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:commodity/symbol ["Symbol is already in use"]}}
          (cdts/put (assoc (attributes) :commodity/symbol "USD")))))) ; exists in the basic context

(dbtest symbol-can-be-duplicated-across-entities
  (with-context
    ; The Personal entity already has USD
    ; The Business entity has only CAD
    (is (:id (cdts/put (assoc (attributes)
                              :commodity/symbol "USD"
                              :commodity/entity (find-entity "Business")))))))

(dbtest name-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:commodity/name ["Name is required"]}}
          (cdts/put (dissoc (attributes) :commodity/name))))))

(dbtest type-is-required
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:commodity/type ["Type is required"]}}
          (cdts/put (dissoc (attributes) :commodity/type))))))

(dbtest type-can-be-stock
  (with-context
    (is (:id (cdts/put (assoc (attributes)
                              :commodity/type :stock))))))

(dbtest type-can-be-mutual-fund
  (with-context
    (is (:id (cdts/put (assoc (attributes)
                              :commodity/type :mutual-fund))))))

(dbtest type-must-be-a-recognized-type
  (with-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:commodity/type ["Type must be currency, mutual-fund, or stock"]}}
          (cdts/put (assoc (attributes) :commodity/type :livestock))))))

(def ^:private find-ctx
  (update-in basic-context
             [:commodities] concat [#:commodity{:entity "Personal"
                                                :name "British Pound"
                                                :symbol "GBP"
                                                :type :currency}
                                    #:commodity{:entity "Business"
                                                :name "Euro"
                                                :symbol "EUR"
                                                :type :currency}]))

(dbtest find-by-entity
  (with-context find-ctx
    (is (= #{#:commodity{:name "United States Dollar"
                         :symbol "USD"
                         :type :currency}
             #:commodity{:name "British Pound"
                         :symbol "GBP"
                         :type :currency}}
           (->> (cdts/select {:commodity/entity (find-entity "Personal")})
                (map #(select-keys % [:commodity/name :commodity/type :commodity/symbol]))
                (into #{})))
        "The commodities for the specified entity are returned")))

(dbtest update-a-commodity
  (with-context
    (let [commodity (find-commodity "USD")
          updated (cdts/put (assoc commodity :commodity/name "US Bucks"))]
      (is (= (:id commodity) (:id updated))
          "The same commodity is returned")
      (is (= "US Bucks" (:commodity/name updated))
          "The result contains the new attributes")
      (is (= "US Bucks" (:commodity/name (cdts/find commodity)))
          "The retrieved commodity has the updated attributes"))))

(dbtest delete-a-commodity
  (with-context
    (let [commodity (find-commodity "USD")]
          (cdts/delete commodity)
      (is (nil? (cdts/find commodity))
          "The commodity cannot be retrieved after delete"))))