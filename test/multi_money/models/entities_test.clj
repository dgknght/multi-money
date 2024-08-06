(ns multi-money.models.entities-test
  (:require [clojure.test :refer [is use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
            [multi-money.helpers :refer [reset-db
                                        dbtest]]
            [multi-money.test-context :refer [with-context
                                              find-user
                                              find-entity
                                              find-commodity]]
            [multi-money.models.entities :as ents]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            [multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(def ^:private context
  {:users [#:user{:email "john@doe.com"
                  :given-name "John"
                  :surname "Doe"
                  :identities {:google "abc123"}}]})

(defn- attributes
  [& [user]]
  #:entity{:name "Personal"
           :owner (or user
                      (find-user "john@doe.com"))})

(dbtest create-an-entity
  (with-context context
    (let [user (find-user "john@doe.com")
          result (ents/put (attributes user))]
      (is (comparable? #:entity{:name "Personal"
                                :owner {:id (:id user)}}
                       result)
          "The result contains the correct attributes")
      (is (:id result)
          "The result contains an :id value"))))

(dbtest entity-name-is-required
  (with-context context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:entity{:name ["Name is required"]}}
          (ents/put (dissoc (attributes)
                            :entity/name))))))

(def ^:private update-context
  (assoc context
         :entities [#:entity{:name "Personal"
                             :owner "john@doe.com"}]
         :commodities [#:commodity{:name "United States Dollar"
                                   :symbol "USD"
                                   :type :currency
                                   :entity "Personal"}]))

(dbtest entity-name-is-unique-for-an-owner
  (with-context update-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors #:entity{:name ["Name is already in use"]}}
          (ents/put (attributes))))))

(def ^:private unique-context
  (update-in update-context
             [:users]
             conj
             #:user{:email "jane@doe.com"
                    :given-name "Jane"
                    :surname "Doe"
                    :identities {:google "def456"}}))

(dbtest entity-name-is-not-unique-across-owners
  (with-context unique-context
    (let [user (find-user "jane@doe.com")
          result (ents/put (attributes user))]
      (is (valid? result) "The result does not indicate any validation errors")
      (is (:id result)
          "An :id is assigned"))))

(dbtest update-an-entity
  (with-context update-context
    (let [entity (find-entity "Personal")
          commodity (find-commodity ["USD" entity])
          updated (ents/put (assoc entity
                                   :entity/name "My Money"
                                   :entity/default-commodity commodity))]
      (is (comparable? #:entity{:name "My Money"
                                :default-commodity (select-keys commodity [:id])}
                       updated)
          "The result contains the updated attributes")
      (is (comparable? #:entity{:name "My Money"
                                :default-commodity (select-keys commodity [:id])}
                       (ents/find entity))
          "A retrieved model has the updated attributes"))))

(def ^:private unique-update-context
  (assoc context
         :entities [#:entity{:name "Personal"
                             :owner "john@doe.com"}
                    #:entity{:name "Business"
                             :owner "john@doe.com"}]))

(dbtest cannot-update-to-an-existing-entity-name-with-same-owner
  (with-context unique-update-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:entity/name ["Name is already in use"]}}
          (-> (find-entity "Business")
              (assoc :entity/name "Personal")
              ents/put)))))

(dbtest fetch-all-entities-for-an-owner
  (with-context update-context
    (is (seq-of-maps-like? [#:entity{:name "Personal"}]
                           (ents/select {:entity/owner (find-user "john@doe.com")})))))

(dbtest delete-an-entity
  (with-context update-context
    (let [entity (find-entity "Personal")]
      (ents/delete entity)
      (is (nil? (ents/find entity))
          "The entity cannot be retrieved after delete"))))

(dbtest get-a-count-of-entities
  (with-context unique-update-context
    (is (= 2 (ents/count {:entity/owner (find-user "john@doe.com")})))))
