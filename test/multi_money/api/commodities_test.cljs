(ns multi-money.api.commodities-test
  (:require [clojure.test :refer [deftest async is]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.state :as state]
            [multi-money.api.commodities :as ents]))

(deftest fetch-commodities
  (async
    done
    (let [calls (atom [])]
      (with-redefs [api/get (fn [& [_url {:keys [on-success]} :as args]]
                              (swap! calls conj args)
                              (on-success [#:commodity{:name "US Dollar"
                                                       :symbol "USD"}
                                           #:commodity{:name "CA Dollar"
                                                       :symbol "CAD"}]))
                    state/current-entity (atom {:id "201"})]
        (ents/select :on-success (fn [commodities]
                                   (is (:dgknght.app-lib.test-assertions/seq-of-maps-like?
                                         [#:commodity{:name "US Dollar"
                                                       :symbol "USD"}
                                           #:commodity{:name "CA Dollar"
                                                       :symbol "CAD"}]
                                         commodities)
                                       "The commodities are returned")
                                   (is (= 1 (count @calls))
                                       "One API call is made")
                                   (is (= "/api/entities/201/commodities"
                                          (ffirst @calls))
                                       "The commodities index API endpoint is called")
                                   (done)))))))

(deftest create-an-commodity
  (async
    done
    (let [calls (atom [])
          commodity {:commodity/name "British Pound"
                     :commodity/symbol "GBP"}]
      (with-redefs [api/post (fn [& [_url commodity {:keys [on-success]} :as args]]
                                (swap! calls conj args)
                                (on-success (assoc commodity :id "202")))]
        (ents/put commodity
                  :on-success (fn [res]
                                (is (dgknght.app-lib.test-assertions/comparable?
                                      {:id "202"}
                                       res)
                                    "The created commodity is returned")
                                (is (= 1 (count @calls))
                                    "One API call is made")
                                (is (= "/api/commodities"
                                       (ffirst @calls))
                                    "The commodities create API endpoint is called")
                                (done)))))))

(deftest update-an-commodity
  (async
    done
    (let [calls (atom [])
          commodity {:id "201"
                  :commodity/name "New Name"}]
      (with-redefs [api/patch (fn [& [_url commodity {:keys [on-success]} :as args]]
                                (swap! calls conj args)
                                (on-success commodity))]
        (ents/put commodity
                  :on-success (fn [res]
                                (is (= commodity
                                       res)
                                    "The updated commodity is returned")
                                (is (= 1 (count @calls))
                                    "One API call is made")
                                (is (= "/api/commodities/201"
                                       (ffirst @calls))
                                    "The commodities update API endpoint is called")
                                (done)))))))

(deftest delete-an-commodity
  (async
    done
    (let [calls (atom [])
          commodity {:id "201"
                  :commodity/name "New Name"}]
      (with-redefs [api/delete (fn [& [_url {:keys [on-success]} :as args]]
                                 (swap! calls conj args)
                                 (on-success commodity))]
        (ents/delete commodity
                     :on-success (fn []
                                   (is (= 1 (count @calls))
                                       "One API call is made")
                                   (is (= "/api/commodities/201"
                                          (ffirst @calls))
                                       "The commodities delete API endpoint is called")
                                   (done)))))))
