(ns multi-money.models.entities-test
  (:require [clojure.test :refer [is use-fixtures]]
            [dgknght.app-lib.test-assertions]
            [multi-money.helpers :refer [reset-db
                                        dbtest]]
            [multi-money.test-context :refer [with-context
                                              find-user
                                              find-entity
                                              #_find-commodity]]
            [multi-money.models.entities :as ents]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            [multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(def ^:private context
  {:users [#:user{:email "john@doe.com"
                  :given-name "John"
                  :surname "Doe"}]})

(dbtest create-an-entity
  (with-context context
    (let [user (find-user "john@doe.com")
          result (ents/put #:entity{:name "Personal"
                                    :owner user})]
      (is (comparable? #:entity{:name "Personal"
                                :owner (:id user)}
                       result)
          "The result contains the correct attributes")
      (is (:id result)
          "The result contains an :id value"))))

(def ^:private update-context
  {:entities [#:entity{:name "Personal"}]
   #_:commodities #_[{:name "United States Dollar"
                  :symbol "USD"
                  :type :currency
                  :entity-id "Personal"}]})

(dbtest update-an-entity
  (with-context update-context
    (let [entity (find-entity "Personal")
          updated (ents/put (assoc entity :entity/name "My Money"))]
      (is (= "My Money"
             (:entity/name updated))
          "The result contains the updated attributes")
      (is (= "My Money"
             (:entity/name (ents/find entity)))
          "A retrieved model has the updated attributes"))))

(dbtest fetch-all-entities-for-an-owner
  (with-context update-context
    (is (seq-of-maps-like? [#:entity{:name "Personal"}]
                           (ents/select {:entity/owner (find-user "john@doe.com")})))))

(dbtest delete-an-entity
  (with-context update-context
    (let [entity (find-entity "Personal")]
      (ents/delete entity)
      (is (nil? (ents/find (:id entity)))
          "The entity cannot be retrieved after delete"))))
