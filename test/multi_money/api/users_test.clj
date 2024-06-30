(ns multi-money.api.users-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.web :refer [path]]
            [dgknght.app-lib.test-assertions]
            [multi-money.test-context :refer [with-context
                                             find-user]]
            [multi-money.helpers :refer [reset-db
                                        request]]))

(use-fixtures :each reset-db)

(def ^:private attr
  #:user{:email "john@doe.com"
         :given-name "John"
         :surname "Doe"})

(deftest get-my-profile
  (with-context
    (let [user (find-user "john@doe.com")
          res (request :get (path :api :me)
                       :user user)]
      (is (http-success? res))
      (is (comparable? attr
                       (:json-body res))
          "The body contains the user profile"))))
