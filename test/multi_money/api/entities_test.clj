(ns multi-money.api.entities-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.pprint :refer [pprint]]
            [ring.mock.request :as req]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.web :refer [path]]
            [multi-money.helpers :refer [request]]
            [multi-money.models.entities :as ents]
            [multi-money.models.users :as usrs]
            [multi-money.handler :refer [app]]))

(defn- criteria->pred
  [criteria]
  (apply every-pred (map (fn [[k v]]
                           (fn [m] (= (get m k) v)))
                         criteria)))

(defmacro with-mocks
  [bindings & body]
  `(let [calls# (atom {:put [] :select []})
         f# (fn* [~(first bindings)] ~@body)]
     (with-redefs [ents/put (fn [& args#]
                              (swap! calls# update-in [:put] conj args#)
                              (update-in (first args#) [:id] (fnil identity "200")))
                   ents/select (fn [& args#]
                                 (swap! calls# update-in [:select] conj args#)
                                 (filter (criteria->pred (first args#))
                                         [{:id "201"
                                           :entity/owner {:id "101"}
                                           :entity/name "Personal"}
                                          {:id "202"
                                           :entity/owner {:id "101"}
                                           :entity/name "Business"}]))
                   usrs/find (fn [id#]
                               (when (= "101" id#)
                                 {:id id#}))]
       (f# calls#))))

(deftest an-authenticated-user-can-create-an-entity
  (with-mocks [calls]
    (let [res (request :post (path :api :entities)
                       :json-body {:name "Personal"}
                       :user {:id "101"})
          {[c :as cs] :put} @calls]
      (is (http-created? res))
      (is (= 1 (count cs))
          "The entities/put fn is called once")
      (is (= [{:entity/name "Personal"
               :entity/owner {:id "101"}}] c)
          "The entities/put fn is called with the correct arguments")
      (is (= {:id "200"
              :entity/name "Personal"
              :entity/owner {:id "101"}}
             (:json-body res))
          "The created entity is returned"))))

(deftest an-unauthenticated-user-cannot-create-an-entity
  (with-mocks [calls]
    (let [res (request :post (path :api :entities)
                       :json-body {:name "Personal"})]
      (is (http-unauthorized? res))
      (is (empty? (:put @calls))
          "The entities/put fn is not called"))))

(deftest an-authenticated-user-can-get-a-list-of-his-entities
  (with-mocks [calls]
    (let [res (request :get (path :api :entities)
                       :user {:id "101"})
          {[c :as cs] :select} @calls]
      (is (http-success? res))
      (is (comparable? {"Content-Type" "application/json; charset=utf-8"}
                       (:headers res))
          "The response has the correct content type")
      (is (seq-of-maps-like? [{:id "201" :entity/name "Personal"}
                              {:id "202" :entity/name "Business"}]
                             (:json-body res))
          "The entity data is returned")
      (is (= 1 (count cs))
          "The ents/select fn is called once")
      (is (= [{:entity/owner-id "101"}] c)
          "The ents/select fn is called with the correct arguments"))))

(deftest an-authenticated-user-can-update-his-entity
  (testing "update an existing entity"
    (with-mocks [calls]
      (let [res (request :patch (path :api :entities "201")
                         :user {:id "101"}
                         :json-body {:name "The new name"})
            {[c :as cs] :put} @calls]
        (is (http-success? res))
        (is (= 1 (count cs))
            "The entities put fn is called once")
        (is (comparable? [{:id "201"
                           :entity/name "The new name"}]
                         c)
            "The entities update fn is called with the updated entity map")
        (is (comparable? {:id "201"
                          :entity/name "The new name"}
                         (:json-body res))
            "The result of the update fn is returned"))))
  (testing "attempt to update an non-existing entity"
    (with-mocks [calls]
      (is (http-not-found? (request :patch (path :api :entities "999")
                                    :user {:id "101"}
                                    :json-body {:name "The new name"})))
      (is (zero? (count (:put @calls)))
          "The entities put fn is not called"))))

(deftest an-authenticate-user-cannot-update-anothers-entity
  (is false "Need to write the test"))

(deftest an-unauthenticated-user-cannot-update-an-entity
  (is false "Need to write the test"))

(deftest delete-an-entity
  (testing "delete an existing entity"
    (let [calls (atom [])]
      (with-redefs [ents/delete (fn [& args]
                                  (swap! calls conj args)
                                  nil)
                    ents/select (constantly [{:id "101"
                                              :name "My Money"}])]
        (let [res (-> (req/request :delete (path :api :entities "101"))
                      app)
              [c :as cs] @calls]
          (is (http-no-content? res))
          (is (= 1 (count cs))
              "The delete function is called once")
          (is (= [{:id "101"
                   :name "My Money"}]
                 c)
              "The delete funtion is called with the correct arguments")))))
  (testing "attempt to delete a non-existing entity"
    (let [calls (atom [])]
      (with-redefs [ents/delete (fn [& args]
                                  (swap! calls conj args)
                                  nil)
                    ents/select (constantly [])]
        (is (http-not-found? (-> (req/request :delete (path :api :entities "101"))
                                 app)))
        (is (zero? (count @calls))
            "The delete fn is not called")))))
