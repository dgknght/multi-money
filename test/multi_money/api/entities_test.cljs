(ns multi-money.api.entities-test
  (:require [clojure.test :refer [deftest async is]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.api.entities :as ents]))

(deftest fetch-entities
  (async
    done
    (let [calls (atom [])]
      (with-redefs [api/get (fn [& [_url {:keys [on-success]} :as args]]
                              (swap! calls conj args)
                              (on-success [#:entity{:name "Business"}
                                           #:entity{:name "Business"}]))]
        (ents/select :on-success (fn [entities]
                                   (is (:dgknght.app-lib.test-assertions/seq-of-maps-like?
                                         [#:entity{:name "Business"}
                                          #:entity{:name "Personal"}]
                                         entities)
                                       "The entities are returned")
                                   (is (= 1 (count @calls))
                                       "One API call is made")
                                   (is (= "/api/entities"
                                          (ffirst @calls))
                                       "The entities index API endpoint is called")
                                   (done)))))))

(deftest create-an-entity
  (async
    done
    (let [calls (atom [])
          entity {:entity/name "New Name"}]
      (with-redefs [api/post (fn [& [_url entity {:keys [on-success]} :as args]]
                                (swap! calls conj args)
                                (on-success (assoc entity :id "202")))]
        (ents/put entity
                  :on-success (fn [res]
                                (is (dgknght.app-lib.test-assertions/comparable?
                                      {:id "202"}
                                       res)
                                    "The created entity is returned")
                                (is (= 1 (count @calls))
                                    "One API call is made")
                                (is (= "/api/entities"
                                       (ffirst @calls))
                                    "The entities create API endpoint is called")
                                (done)))))))

(deftest update-an-entity
  (async
    done
    (let [calls (atom [])
          entity {:id "201"
                  :entity/name "New Name"}]
      (with-redefs [api/patch (fn [& [_url entity {:keys [on-success]} :as args]]
                                (swap! calls conj args)
                                (on-success entity))]
        (ents/put entity
                  :on-success (fn [res]
                                (is (= entity
                                       res)
                                    "The updated entity is returned")
                                (is (= 1 (count @calls))
                                    "One API call is made")
                                (is (= "/api/entities/201"
                                       (ffirst @calls))
                                    "The entities update API endpoint is called")
                                (done)))))))

(deftest delete-an-entity
  (async
    done
    (let [calls (atom [])
          entity {:id "201"
                  :entity/name "New Name"}]
      (with-redefs [api/delete (fn [& [_url {:keys [on-success]} :as args]]
                                 (swap! calls conj args)
                                 (on-success entity))]
        (ents/delete entity
                     :on-success (fn []
                                   (is (= 1 (count @calls))
                                       "One API call is made")
                                   (is (= "/api/entities/201"
                                          (ffirst @calls))
                                       "The entities delete API endpoint is called")
                                   (done)))))))
