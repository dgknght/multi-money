(ns multi-money.api.entities-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.web :refer [path]]
            [multi-money.test-context :refer [with-context
                                              find-user
                                              find-entity]]
            [multi-money.helpers :refer [reset-db
                                         request]]
            [multi-money.models.entities :as ents]))

(use-fixtures :each reset-db)

(deftest an-authenticated-user-can-create-an-entity
  (with-context
    (let [res (request :post (path :api :entities)
                       :json-body {:entity/name "Side Hustle"}
                       :user (find-user "john@doe.com"))]
      (is (http-created? res))
      (is (:id (:json-body res))
          "The response includes an id")
      (is (comparable? {:entity/name "Side Hustle"}
                       (:json-body res))
          "The created entity is returned")
      (is (comparable? {:entity/name "Side Hustle"}
                       (ents/find (:json-body res))) 
          "The entity can be retrieved"))))

(deftest an-invalid-create-request-is-returned-with-errors
  (with-context
    (let [res (request :post (path :api :entities)
                       :user (find-user "john@doe.com")
                       :json-body {:size "large"})]
      (is (http-unprocessable? res))
      (is (= {:errors {:entity/name ["Name is required"]}}
             (:json-body res))
          "The response body contains the validation errors"))))

(deftest an-unauthenticated-user-cannot-create-an-entity
  (let [count-before (ents/count)]
    (is (http-unauthorized? (request :post (path :api :entities)
                                     :json-body {:entity/name "My Money"})))
    (is (= count-before (ents/count))
        "No entity is created")))

(deftest an-authenticated-user-can-get-a-list-of-his-entities
  (with-context
    (let [res (request :get (path :api :entities)
                       :user (find-user "john@doe.com"))]
      (is (http-success? res))
      (is (comparable? {"Content-Type" "application/json; charset=utf-8"}
                       (:headers res))
          "The response has the correct content type")
      (is (seq-of-maps-like? [{:entity/name "Personal"}
                              {:entity/name "Business"}]
                             (:json-body res))
          "The entities are returned"))))

(deftest an-authenticated-user-can-update-his-entity
  (with-context
    (testing "update an existing entity"
      (let [entity (find-entity "Personal")
            res (request :patch (path :api :entities (:id entity))
                         :user (find-user "john@doe.com")
                         :json-body {:entity/name "The new name"})]
        (is (http-success? res))
        (is (comparable? {:entity/name "The new name"}
                         (:json-body res))
            "The updated entity is returned")
        (is (comparable? {:entity/name "The new name"}
                         (ents/find entity))
            "The updated entity can be retrieved")))
    (testing "attempt to update an non-existing entity"
      (is (http-not-found? (request :patch (path :api :entities "999")
                                    :user (find-user "john@doe.com")
                                    :json-body {:name "The new name"}))))))

(deftest an-authenticate-user-cannot-update-anothers-entity
  (with-context
    (let [entity (find-entity "Personal")]
      (is (http-not-found? (request :patch (path :api :entities (:id entity))
                                    :user (find-user "jane@doe.com")
                                    :json-body {:name "The new name"})))
      (is (= (:entity/name entity)
             (:entity/name (ents/find entity)))
          "The name is not updated in the database"))))

(deftest an-unauthenticated-user-cannot-update-an-entity
  (with-context
    (let [entity (find-entity "Personal")]
      (is (http-unauthorized? (request :patch (path :api :entities (:id entity))
                                       :json-body {:name "The new name"})))
      (is (not= "The new name"
                (:name (ents/find entity)))
          "The name is not updated in the database"))))

(deftest delete-an-entity
  (with-context
    (testing "delete an existing entity"
      (let [entity (find-entity "Personal")
            res (request :delete (path :api :entities (:id entity))
                         :user (find-user "john@doe.com"))]
        (is (http-no-content? res))
        (is (nil? (ents/find entity))
            "The entity cannot be retrieved after delete")))
    (testing "attempt to delete a non-existing entity"
      (is (http-not-found? (request :delete (path :api :entities "999")
                                    :user (find-user "john@doe.com")))))))

(deftest an-authenticated-user-cannot-delete-anothers-entity
  (with-context
    (let [entity (find-entity "Personal")]
      (is (http-not-found? (request :delete (path :api :entities (:id entity))
                                    :user (find-user "jane@doe.com"))))
      (is (ents/find entity)
          "The entity can be retrieved after denied delete request"))))

(deftest an-unauthenticated-user-cannot-delete-an-entity
  (with-context
    (let [entity (find-entity "Personal")]
      (is (http-unauthorized? (request :delete (path :api :entities (:id entity)))))
      (is (ents/find entity)
          "The entity can be retrieved after denied delete request"))))
