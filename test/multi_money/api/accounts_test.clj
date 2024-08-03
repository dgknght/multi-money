(ns multi-money.api.accounts-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.web :refer [path]]
            [multi-money.test-context :refer [with-context
                                              find-user
                                              find-entity
                                              find-commodity
                                              find-account]]
            [multi-money.helpers :refer [reset-db
                                         request]]
            [multi-money.models.accounts :as acts]))

(use-fixtures :each reset-db)

(defn- attributes
  [& [commodity]]
  (cond-> {:account/name "My New Account"
           :account/type :asset}
    commodity (assoc :account/commodity (select-keys commodity [:id]))))

(defn- create-account
  [& {:keys [entity commodity user prep-fn no-user?]
      :or {prep-fn identity}}]
  (let [entity (or entity (find-entity "Personal"))
        user (or user
                 (when-not no-user?
                   (find-user "john@doe.com")))
        attr (attributes commodity)]
    (request :post (path :api :entities (:id entity) :accounts)
             :json-body (prep-fn attr)
             :user user)))

(deftest an-authenticated-user-can-create-an-account-in-his-entity
  (with-context
    (let [commodity (find-commodity "USD" "Personal")
          res (create-account
                :entity (find-entity "Personal")
                :commodity commodity
                :user (find-user "john@doe.com"))]
      (is (http-created? res))
      (is (:id (:json-body res))
          "The response includes an id")
      (is (comparable? {:account/name "My New Account"
                        :account/type "asset"
                        :account/commodity (select-keys commodity [:id])}
                       (:json-body res))
          "The created account is returned")
      (is (comparable? {:account/name "My New Account"
                        :account/type :asset
                        :account/commodity (select-keys commodity [:id])}
                       (acts/find (:json-body res))) 
          "The account can be retrieved"))))

(deftest an-invalid-create-request-is-returned-with-errors
  (with-context
    (let [res (create-account :prep-fn #(dissoc % :account/type))]
      (is (http-unprocessable? res))
      (is (= {:errors {:account/type ["Type is required"]}}
             (:json-body res))
          "The response body contains the validation errors"))))

(deftest an-authenticated-user-cannot-create-an-account-in-anothers-entity
  (with-context
    (let [count-before (acts/count)]
      (is (http-not-found? (create-account
                             :user (find-user "jane@doe.com"))))
      (is (= count-before (acts/count))
          "No account is created"))))

(deftest an-unauthenticated-user-cannot-create-an-account
  (with-context
    (let [count-before (acts/count)]
      (is (http-unauthorized? (create-account :no-user? true)))
      (is (= count-before (acts/count))
          "No account is created"))))

(defn- get-accounts
  [& {:keys [entity user no-user?]}]
  (let [user (or user
                 (when-not no-user?
                   (find-user "john@doe.com")))
        entity (or entity (find-entity "Personal"))]
    (request :get (path :api :entities (:id entity) :accounts)
             :user user)))

(deftest an-authenticated-user-can-get-a-list-of-his-accounts
  (with-context
    (let [res (get-accounts)]
      (is (http-success? res))
      (is (comparable? {"Content-Type" "application/json; charset=utf-8"}
                       (:headers res))
          "The response has the correct content type")
      (is (= #{{:account/name "Checking"
                :account/type "asset"}
               {:account/name "Salary"
                :account/type "income"}
               {:account/name "Rent"
                :account/type "expense"}
               {:account/name "Groceries"
                :account/type "expense"}
               {:account/name "Credit Card"
                :account/type "liability"}}
             (->> (:json-body res)
                  (map #(select-keys % [:account/name :account/type]))
                  set))
          "The accounts are returned"))))

(deftest an-authenticated-user-cannot-get-a-list-of-accounts-in-anothers-entity
  (with-context
    (let [res (get-accounts :user (find-user "jane@doe.com"))]
      (is (http-success? res))
      (is (empty? (:json-body res))
          "No accounts are returned"))))

(deftest an-unauthenticated-user-cannot-get-a-list-of-accounts
  (with-context
    (is (http-unauthorized? (get-accounts :no-user? true)))))

(defn- update-account
  [account & {:keys [user no-user?]}]
  (let [user (or user
                 (when-not no-user?
                   (find-user "john@doe.com")))]
    (request :patch (path :api :accounts (:id account))
             :user user 
             :json-body (dissoc account :id))))

(deftest an-authenticated-user-can-update-his-account
  (with-context
    (testing "update an existing account"
      (let [account (find-account "Checking")
            res (update-account (assoc account :account/name "The new name"))]
        (is (http-success? res))
        (is (comparable? {:account/name "The new name"}
                         (:json-body res))
            "The updated account is returned")
        (is (comparable? {:account/name "The new name"}
                         (acts/find account))
            "The updated account can be retrieved")))
    (testing "attempt to update an non-existing account"
      (is (http-not-found? (request :patch (path :api :accounts "999")
                                    :user (find-user "john@doe.com")
                                    :json-body {:name "The new name"}))))))

(deftest an-authenticate-user-cannot-update-anothers-account
  (with-context
    (let [account (find-account "Checking")]
      (is (http-not-found? (update-account (assoc account :account/name "The new name")
                                           :user (find-user "jane@doe.com"))))
      (is (= (:account/name account)
             (:account/name (acts/find account)))
          "The name is not updated in the database"))))

(deftest an-unauthenticated-user-cannot-update-an-account
  (with-context
    (let [account (find-account "Checking")]
      (is (http-unauthorized? (update-account (assoc account :account/name "The new name")
                                           :no-user? true)))
      (is (= (:account/name account)
             (:account/name (acts/find account)))
          "The name is not updated in the database"))))

(defn- delete-account
  [account & {:keys [user no-user?]}]
  (let [user (or user
                 (when-not no-user?
                   (find-user "john@doe.com")))]
    (request :delete (path :api :accounts (:id account))
             :user user)))

(deftest an-authenticated-user-can-delete-an-account-in-his-entity
  (with-context
    (testing "delete an existing account"
      (let [account (find-account "Checking")
            res (delete-account account)]
        (is (http-no-content? res))
        (is (nil? (acts/find account))
            "The account cannot be retrieved after delete")))
    (testing "attempt to delete a non-existing account"
      (is (http-not-found? (delete-account {:id "999"}))))))

(deftest an-authenticated-user-cannot-delete-anothers-account
  (with-context
    (let [account (find-account "Checking")]
      (is (http-not-found? (delete-account account
                                           :user (find-user "jane@doe.com"))))
      (is (acts/find account)
          "The account can be retrieved after denied delete request"))))

(deftest an-unauthenticated-user-cannot-delete-an-account
  (with-context
    (let [account (find-account "Checking")]
      (is (http-unauthorized? (delete-account account
                                              :no-user? true)))
      (is (acts/find account)
          "The account can be retrieved after denied delete request"))))
