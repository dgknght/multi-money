(ns multi-money.api.commodities-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.web :refer [path]]
            [multi-money.helpers :refer [request
                                         criteria->pred]]
            [multi-money.db :as db]
            [multi-money.models.commodities :as ents]
            [multi-money.models.users :as usrs]))

(defmacro with-mocks
  [bindings & body]
  `(let [calls# (atom {:put [] :select []})
         f# (fn* [~(first bindings)] ~@body)]
     (with-redefs [ents/put (fn [& args#]
                              (swap! calls# update-in [:put] conj args#)
                              (update-in (first args#) [:id] (fnil idcommodity "200")))
                   ents/select (fn [& args#]
                                 (swap! calls# update-in [:select] conj args#)
                                 (filter (criteria->pred (first args#))
                                         [{:id "201"
                                           :commodity/entity {:id "101"}
                                           :commodity/name "Personal"}
                                          {:id "202"
                                           :commodity/entity {:id "101"}
                                           :commodity/name "Business"}]))
                   ents/delete (fn [& args#]
                                 (swap! calls# update-in [:delete] conj args#))
                   usrs/find (fn [id#]
                               (when (#{"101" "102"} id#)
                                 {:id id#}))]
       (f# calls#))))

(deftest an-authenticated-user-can-create-an-commodity
  (with-mocks [calls]
    (let [res (request :post (path :api :commodities)
                       :json-body {:name "Personal"}
                       :user {:id "101"})
          {[c :as cs] :put} @calls]
      (is (http-created? res))
      (is (= 1 (count cs))
          "The commodities/put fn is called once")
      (is (= [{:commodity/name "Personal"
               :commodity/entity {:id "101"}}] c)
          "The commodities/put fn is called with the correct arguments")
      (is (= {:id "200"
              :commodity/name "Personal"
              :commodity/entity {:id "101"}}
             (:json-body res))
          "The created commodity is returned"))))

(deftest an-create-request-is-returned-with-errors
  (let [calls (atom [])]
    (with-redefs [db/put (fn [& args] (swap! calls conj args))
                  ents/select (fn [& _] [])
                  usrs/find (fn [id] {:id id})]
      (let [res (request :post (path :api :commodities)
                         :user {:id "101"}
                         :json-body {:size "large"})]
        (is (http-unprocessable? res))
        (is (= {:errors {:commodity/name ["Name is required"]}}
               (:json-body res))
            "The response body contains the validation errors")
        (is (empty? @calls)
            "Nothing is passed to the database")))))

(deftest an-unauthenticated-user-cannot-create-an-commodity
  (with-mocks [calls]
    (is (http-unauthorized? (request :post (path :api :commodities)
                                     :json-body {:name "Personal"})))
    (is (empty? (:put @calls))
        "The commodities/put fn is not called")))

(deftest an-authenticated-user-can-get-a-list-of-his-commodities
  (with-mocks [calls]
    (let [res (request :get (path :api :commodities)
                       :user {:id "101"})
          {[c :as cs] :select} @calls]
      (is (http-success? res))
      (is (comparable? {"Content-Type" "application/json; charset=utf-8"}
                       (:headers res))
          "The response has the correct content type")
      (is (seq-of-maps-like? [{:id "201" :commodity/name "Personal"}
                              {:id "202" :commodity/name "Business"}]
                             (:json-body res))
          "The commodity data is returned")
      (is (= 1 (count cs))
          "The ents/select fn is called once")
      (is (= [{:commodity/entity "101"}] c)
          "The ents/select fn is called with the correct arguments"))))

(deftest an-authenticated-user-can-update-his-commodity
  (testing "update an existing commodity"
    (with-mocks [calls]
      (let [res (request :patch (path :api :commodities "201")
                         :user {:id "101"}
                         :json-body {:name "The new name"})
            {[c :as cs] :put} @calls]
        (is (http-success? res))
        (is (= 1 (count cs))
            "The commodities put fn is called once")
        (is (comparable? [{:id "201"
                           :commodity/name "The new name"}]
                         c)
            "The commodities update fn is called with the updated commodity map")
        (is (comparable? {:id "201"
                          :commodity/name "The new name"}
                         (:json-body res))
            "The result of the update fn is returned"))))
  (testing "attempt to update an non-existing commodity"
    (with-mocks [calls]
      (is (http-not-found? (request :patch (path :api :commodities "999")
                                    :user {:id "101"}
                                    :json-body {:name "The new name"})))
      (is (empty? (:put @calls))
          "The commodities put fn is not called"))))

(deftest an-authenticate-user-cannot-update-anothers-commodity
  (with-mocks [calls]
    (is (http-not-found? (request :patch (path :api :commodities "201")
                                  :user {:id "102"}
                                  :json-body {:name "The new name"})))
    (is (empty? (:put @calls))
        "The commodities put fn is called once")))

(deftest an-unauthenticated-user-cannot-update-an-commodity
  (with-mocks [calls]
    (is (http-unauthorized? (request :patch (path :api :commodities "201")
                                  :json-body {:name "The new name"})))
    (is (empty? (:put @calls))
        "The commodities put fn is called once")))

(deftest delete-an-commodity
  (testing "delete an existing commodity"
    (with-mocks [calls]
      (let [res (request :delete (path :api :commodities "201")
                         :user {:id "101"})
            {[c :as cs] :delete} @calls]
        (is (http-no-content? res))
        (is (= 1 (count cs))
            "The delete function is called once")
        (is (= [{:id "201"
                 :commodity/entity {:id "101"}
                 :commodity/name "Personal"}]
               c)
            "The delete function is called with the commodity to be deleted"))))
  (testing "attempt to delete a non-existing commodity"
    (with-mocks [calls]
      (is (http-not-found? (request :delete (path :api :commodities "999")
                                    :user {:id "101"})))
      (is (empty? (:delete @calls))
          "The delete fn is not called"))))

(deftest an-authenticated-user-cannot-delete-anothers-commodity
  (with-mocks [calls]
    (is (http-not-found? (request :delete (path :api :commodities "201")
                                  :user {:id "102"})))
    (is (empty? (:delete @calls))
        "The delete fn is not called")))

(deftest an-unauthenticated-user-cannot-delete-an-commodity
  (with-mocks [calls]
    (is (http-unauthorized? (request :delete (path :api :commodities "201"))))
    (is (empty? (:delete @calls))
        "The delete fn is not called")))
