(ns multi-money.api.commodities-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.web :refer [path]]
            [multi-money.test-context :refer [with-context
                                              basic-context
                                              find-user
                                              find-entity
                                              find-commodity]]
            [multi-money.helpers :refer [request
                                         reset-db]]
            [multi-money.models.commodities :as cdts]))

(use-fixtures :each reset-db)

(defn- attributes
  [entity]
  #:commodity{:name "British Pound"
              :type :currency
              :symbol "GBP"
              :entity (select-keys entity [:id])})

(deftest an-authenticated-user-can-create-a-commodity
  (with-context
    (let [user (find-user "john@doe.com")
          entity (find-entity "Personal")
          attr (attributes entity)
          res (request :post (path :api :commodities)
                       :json-body attr
                       :user user)]
      (is (http-created? res))
      (is (:id (:json-body res))
          "The response contains an id")
      (is (comparable? (update-in attr [:commodity/type] name) ; JSON doesn't know about keywords
                       (:json-body res))
          "The created commodity is returned")
      (is (comparable? attr (cdts/find (:json-body res)))
          "The commodity can be retrieved"))))

(deftest an-invalid-create-request-is-returned-with-errors
  (with-context
    (let [res (request :post (path :api :commodities)
                       :user (find-user "john@doe.com")
                       :json-body {:size "large"})]
      (is (http-unprocessable? res))
      (is (= {:errors #:commodity{:name ["Name is required"]
                                  :symbol ["Symbol is required"]
                                  :type ["Type is required"]
                                  :entity ["Entity is required"]}}
             (:json-body res))
          "The response body contains the validation errors"))))

(deftest an-unauthenticated-user-cannot-create-an-commodity
  (let [count-before (cdts/count)]
    (is (http-unauthorized? (request :post (path :api :commodities)
                                     :json-body {:commodity/name "British Pound"})))
    (is (= count-before (cdts/count))
        "No commodity is created")))

(def ^:private list-context
  (update-in basic-context
             [:commodities] concat [{:commodity/symbol "JPY"
                                     :commodity/name "Japanese Yen"
                                     :commodity/type :currency
                                     :commodity/entity "Personal"}]))

(deftest an-authenticated-user-can-get-a-list-of-commodities-in-his-entity
  (with-context list-context
    (let [res (request :get (path :api :commodities)
                       :user (find-user "john@doe.com"))]
      (is (http-success? res))
      (is (comparable? {"Content-Type" "application/json; charset=utf-8"}
                       (:headers res))
          "The response has the correct content type")
      (is (seq-of-maps-like? [{:commodity/name "United States Dollar"}
                              {:commodity/name "Japanese Yen"}]
                             (:json-body res))
          "The commodities are returned"))))

(deftest an-authenticated-user-can-update-a-commodity-in-his-entity
  (testing "update an existing commodity"
    (with-context
      (let [commodity (find-commodity "USD")
            res (request :patch (path :api :commodities (:id commodity))
                         :user (find-user "john@doe.com")
                         :json-body {:commodity/name "US Clams"})]
        (is (http-success? res))
        (is (comparable? {:commodity/name "US Clams"}
                         (:json-body res))
            "The updated commodity is returned")
        
        (is (comparable? {:commodity/name "US Clams"}
                         (cdts/find (:json-body res)))
            "The updated commodity can be retrieved"))))
  (testing "attempt to update an non-existing commodity"
    (with-context
      (is (http-not-found? (request :patch (path :api :commodities "999")
                                    :user (find-user "john@doe.com")
                                    :json-body {:name "US Clams"}))))))

(deftest an-authenticate-user-cannot-update-anothers-commodity
  (with-context list-context
    (is (http-not-found? (request :patch (path :api :commodities (:id (find-commodity "USD")))
                                  :user (find-user "jane@doe.com")
                                  :json-body {:commodity/name "The new name"})))
    (is (comparable? {:commodity/name "United States Dollar"}
                     (cdts/find-by {:commodity/symbol "USD"}))
        "The commodity is not updated in the database")))

(deftest an-unauthenticated-user-cannot-update-an-commodity
  (with-context
    (is (http-unauthorized? (request :patch (path :api :commodities (:id (find-commodity "USD")))
                                     :json-body {:name "The new name"})))
    (is (comparable? {:commodity/name "United States Dollar"}
                     (cdts/find-by {:commodity/symbol "USD"}))
        "The commodity is not updated in the database")))

(deftest delete-an-commodity
  (testing "delete an existing commodity"
    (with-context list-context
      (let [commodity (find-commodity "CAD")
            res (request :delete (path :api :commodities (:id commodity))
                         :user (find-user "john@doe.com"))]
        (is (http-no-content? res))
        (is (nil? (cdts/find commodity))
            "The commodity cannot be retrieved after delete"))))
  (testing "attempt to delete a non-existing commodity"
    (with-context
      (is (http-not-found? (request :delete (path :api :commodities "999")
                                    :user (find-user "john@doe.com")))))))

(deftest an-authenticated-user-cannot-delete-anothers-commodity
  (with-context list-context
    (let [commodity (find-commodity "USD")]
      (is (http-not-found? (request :delete (path :api :commodities (:id commodity))
                                    :user (find-user "jane@doe.com"))))
      (is (comparable? {:commodity/name "United States Dollar"}
                       (cdts/find commodity))
          "The commodity can be retrieved after rejected delete request"))))

(deftest an-unauthenticated-user-cannot-delete-a-commodity
  (with-context
    (let [commodity (find-commodity "USD")]
      (is (http-unauthorized? (request :delete (path :api :commodities (:id commodity)))))
      (is (comparable? {:commodity/name "United States Dollar"}
                       cdts/find commodity)
          "The commodity can be retrieved after rejected delete request"))))
