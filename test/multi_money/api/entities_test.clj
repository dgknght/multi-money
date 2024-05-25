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
                       :json-body {:entity/name "Business"}
                       :user (find-user "john@doe.com"))]
      (is (http-created? res))
      (is (:id (:json-body res))
          "The response includes an id")
      (is (comparable? {:entity/name "Business"}
                       (:json-body res))
          "The created entity is returned")
      (is (comparable? {:entity/name "Business"}
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
  (with-mocks [calls]
    (let [count-before (ents/count)]
      (is (http-unauthorized? (request :post (path :api :entities)
                                     :json-body {:entity/name "My Money"})))
      (is (= count-before (ents/count))
          "No entity is created"))))

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
  (testing "update an existing entity"
    (with-context
      (let [entity (find-entity "Personal")
            res (request :patch (path :api :entities "201")
                         :user (find-user "john@doe.com")
                         :json-body {:name "The new name"})]
        (is (http-success? res))
        (is (comparable? [{:entity/name "The new name"}]
                         (ents/find entity))
            "The updated entity can be retrieved")
        (is (comparable? {:id "201"
                          :entity/name "The new name"}
                         (:json-body res))
            "The updated entity is returned"))))
  (testing "attempt to update an non-existing entity"
    (with-context
      (is (http-not-found? (request :patch (path :api :entities "999")
                                    :user (find-user "john@doe.com")
                                    :json-body {:name "The new name"}))))))

(deftest an-authenticate-user-cannot-update-anothers-entity
  (with-mocks [calls]
    (is (http-not-found? (request :patch (path :api :entities "201")
                                  :user {:id "102"}
                                  :json-body {:name "The new name"})))
    (is (empty? (:put @calls))
        "The entities put fn is called once")))

(deftest an-unauthenticated-user-cannot-update-an-entity
  (with-mocks [calls]
    (is (http-unauthorized? (request :patch (path :api :entities "201")
                                  :json-body {:name "The new name"})))
    (is (empty? (:put @calls))
        "The entities put fn is called once")))

(deftest delete-an-entity
  (testing "delete an existing entity"
    (with-mocks [calls]
      (let [res (request :delete (path :api :entities "201")
                         :user {:id "101"})
            {[c :as cs] :delete} @calls]
        (is (http-no-content? res))
        (is (= 1 (count cs))
            "The delete function is called once")
        (is (= [{:id "201"
                 :entity/owner {:id "101"}
                 :entity/name "Personal"}]
               c)
            "The delete function is called with the entity to be deleted"))))
  (testing "attempt to delete a non-existing entity"
    (with-mocks [calls]
      (is (http-not-found? (request :delete (path :api :entities "999")
                                    :user {:id "101"})))
      (is (empty? (:delete @calls))
          "The delete fn is not called"))))

(deftest an-authenticated-user-cannot-delete-anothers-entity
  (with-mocks [calls]
    (is (http-not-found? (request :delete (path :api :entities "201")
                                  :user {:id "102"})))
    (is (empty? (:delete @calls))
        "The delete fn is not called")))

(deftest an-unauthenticated-user-cannot-delete-an-entity
  (with-mocks [calls]
    (is (http-unauthorized? (request :delete (path :api :entities "201"))))
    (is (empty? (:delete @calls))
        "The delete fn is not called")))
