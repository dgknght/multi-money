(ns multi-money.api.users-test
  (:require [clojure.test :refer [deftest async is]]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.api.users :as usrs]))

(deftest fetch-my-profile
  (async
    done
    (let [calls (atom [])]
      (with-redefs [api/get (fn [& [_url {:keys [on-success]} :as args]]
                              (swap! calls conj args)
                              (on-success #:user{:email "john@doe.com"}))]
        (usrs/me :on-success (fn [user]
                               (is (= 1 (count @calls))
                                   "The user API endpoint is called once")
                               (is (= "/api/me" (ffirst @calls))
                                   "The profile endpoint is called")
                               (is (= #:user{:email "john@doe.com"}
                                      user))
                               (done)))))))
