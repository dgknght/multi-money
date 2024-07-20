(ns multi-money.datalog-test
  (:require [clojure.test :refer [deftest is]]
            [stowaway.datalog :as s]
            [dgknght.app-lib.test-assertions]
            [multi-money.datalog :as dtl]))

(deftest apply-a-criteria
  (let [calls (atom [])
        orig s/apply-criteria
        query {:find ['?x]}
        criteria #:entity{:name "Personal"}]
    (with-redefs [s/apply-criteria (fn [& args]
                                     (swap! calls conj args)
                                     (apply orig args))]
      (is (= '{:find [?x]
               :where [[?x :entity/name ?a]]
               :in [?a]
               :args ["Personal"]}
             (dtl/apply-criteria query criteria)))
      (let [[c :as cs] @calls]
        (is (= 1 (count cs))
            "The stowaway library is called once")
        (is (comparable? [query
                          criteria
                          {:relationships #{[:user :entity]
                                            [:entity :commodity]}
                           :query-prefix [:query]}]
                         c)
            "The relationships and query prefix are specified in the call to the stowaway library.")))))
