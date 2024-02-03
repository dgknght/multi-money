(ns multi-money.api.users-test
  (:require [clojure.test :refer [deftest async is]]
            [multi-money.api.users :as usrs]))

(deftest fetch-my-profile
  (async
    done
    (usrs/me :on-success (fn [user]
                           (is (= #:user{:email "john@doe.com"}
                                  user))
                           (done))
             :on-error (fn [e]
                         (is false e)
                         (done)))))
