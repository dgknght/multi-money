(ns multi-money.api.accounts-test
  (:require [clojure.test :refer [deftest async is]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.state :as state]
            [multi-money.api.accounts :as acts]))

(deftest fetch-accounts
  (async
    done
    (let [calls (atom [])]
      (with-redefs [api/get (fn [& [_url {:keys [on-success]} :as args]]
                              (swap! calls conj args)
                              (on-success [#:account{:name "Checking"
                                                     :type "asset"}
                                           #:account{:name "Salary"
                                                     :type "income"}]))
                    state/current-entity (atom {:id "101"})]
        (acts/select :on-success (fn [accounts]
                                   (is (:dgknght.app-lib.test-assertions/seq-of-maps-like?
                                         [#:account{:name "Checking"
                                                    :type :asset}
                                          #:account{:name "Salary"
                                                    :type :income}]
                                         accounts)
                                       "The accounts are returned")
                                   (is (= 1 (count @calls))
                                       "One API call is made")
                                   (is (= "/api/entities/101/accounts"
                                          (ffirst @calls))
                                       "The accounts index API endpoint is called")
                                   (done)))))))

(deftest create-an-account
  (async
    done
    (let [calls (atom [])
          account #:account{:name "Receivables"
                            :type :asset}]
      (with-redefs [api/post (fn [& [_url account {:keys [on-success]} :as args]]
                               (swap! calls conj args)
                               (on-success (assoc account :id "202")))
                    state/current-entity (atom {:id "101"})]
        (acts/put account
                  :on-success (fn [res]
                                (is (dgknght.app-lib.test-assertions/comparable?
                                      {:id "202"}
                                      res)
                                    "The created account is returned")
                                (is (= 1 (count @calls))
                                    "One API call is made")
                                (is (= "/api/entities/101/accounts"
                                       (ffirst @calls))
                                    "The accounts create API endpoint is called")
                                (done)))))))

(deftest update-an-account
  (async
    done
    (let [calls (atom [])
          account {:id "201"
                   :account/name "New Name"
                   :account/type :liability}]
      (with-redefs [api/patch (fn [& [_url account {:keys [on-success]} :as args]]
                                (swap! calls conj args)
                                (on-success account))]
        (acts/put account
                  :on-success (fn [res]
                                (is (= account
                                       res)
                                    "The updated account is returned")
                                (is (= 1 (count @calls))
                                    "One API call is made")
                                (is (= "/api/accounts/201"
                                       (ffirst @calls))
                                    "The accounts update API endpoint is called")
                                (done)))))))

(deftest delete-an-account
  (async
    done
    (let [calls (atom [])
          account {:id "201"
                   :account/name "New Name"}]
      (with-redefs [api/delete (fn [& [_url {:keys [on-success]} :as args]]
                                 (swap! calls conj args)
                                 (on-success account))]
        (acts/delete account
                     :on-success (fn []
                                   (is (= 1 (count @calls))
                                       "One API call is made")
                                   (is (= "/api/accounts/201"
                                          (ffirst @calls))
                                       "The accounts delete API endpoint is called")
                                   (done)))))))
