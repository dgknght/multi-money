(ns multi-money.datalog-test
  (:require [clojure.test :refer [deftest testing is]]
            [multi-money.datalog :as dtl]))

(def ^:private query '{:find [?x]})

(deftest apply-a-simple-criterion
  (is (= '{:find [?x]
           :where [[?x :entity/name ?name-in]]
           :in [?name-in]
           :args ["Personal"]}
         (dtl/apply-criteria query
                             #:entity{:name "Personal"}))))

(deftest apply-a-simple-id-criterion
  (is (= '{:find [(pull ?x [*])]
           :where [[?x :entity/name ?name]]
           :in [?x]
           :args ["101"]}
         (dtl/apply-criteria '{:find [(pull ?x [*])]
                               :where [[?x :entity/name ?name]]}
                             {:id "101"}))))

(deftest apply-id-criterion-with-predicate
  (is (= '{:find [(pull ?x [*])]
           :where [[?x :entity/name ?name]
                   [(!= ?x ?x-in)]]
           :in [?x-in]
           :args ["101"]}
         (dtl/apply-criteria '{:find [(pull ?x [*])]
                               :where [[?x :entity/name ?name]]}
                             {:id [:!= "101"]}))))

(deftest specify-the-args-key
  (is (= '{:find [?x]
           :where [[?x :entity/name ?name-in]]
           :in [?name-in]
           :mny/args ["Personal"]}
         (dtl/apply-criteria query
                             #:entity{:name "Personal"}
                             :args-key [:mny/args]))))

(deftest specify-the-query-key-prefix
  (is (= {:query '{:find [?x]
                   :where [[?x :entity/name ?name-in]]
                   :in [?name-in]}
          :args ["Personal"]}
         (dtl/apply-criteria {:query query}
                             #:entity{:name "Personal"}
                             :query-prefix [:query]))))

(deftest apply-a-remapped-simple-criterion
  (is (= '{:find [?x]
           :where [[?x :xt/id ?id-in]]
           :in [?id-in]
           :args [123]}
         (dtl/apply-criteria query
                             {:id 123}
                             :remap {:id :xt/id}))))

(deftest apply-a-comparison-criterion
  (is (= '{:find [?x]
           :where [[?x :account/balance ?balance]
                   [(>= ?balance ?balance-in)]]
           :in [?balance-in]
           :args [500M]}
         (dtl/apply-criteria query
                             #:account{:balance [:>= 500M]}))))

(deftest apply-a-not-equal-criterion
  (is (= '{:find [?x]
           :where [[?x :user/name ?name]
                   [(!= ?name ?name-in)]]
           :in [?name-in]
           :args ["John"]}
         (dtl/apply-criteria query
                             #:user{:name [:!= "John"]}))))

(deftest apply-an-intersection-criterion
  (is (= '{:find [?x]
           :where [[?x :transaction/transaction-date ?transaction-date]
                   [(>= ?transaction-date ?transaction-date-in-1)]
                   [(< ?transaction-date ?transaction-date-in-2)]]
           :in [?transaction-date-in-1 ?transaction-date-in-2]
           :args ["2020-01-01" "2020-02-01"]}
         (dtl/apply-criteria query
                             #:transaction{:transaction-date [:and
                                                              [:>= "2020-01-01"]
                                                              [:< "2020-02-01"]]}))
      "statements are added directly to the where chain"))

(deftest apply-a-tuple-matching-criterion
  ; here it's necessary to use the := operator explicitly so that
  ; the query logic doesn't mistake :google for the operator
  (is (= '{:find [?x]
           :where [[?x :user/identities ?identities-in]]
           :in [?identities-in]
           :args [[:google "abc123"]]}
         (dtl/apply-criteria query
                             #:user{:identities [:= [:google "abc123"]]}))))

(deftest apply-options
  (testing "limit"
    (is (= '{:find [?x]
             :limit 1}
           (dtl/apply-options query
                              {:limit 1}))
        "The limit attribute is copied"))
  (testing "sorting"
    (is (= '{:find [?x ?size]
             :where [[?x :shirt/size ?size]]
             :order-by [[?size :asc]]}
           (dtl/apply-options query
                              {:order-by :shirt/size}))
        "A single column is symbolized and ascended is assumed")
    (is (= '{:find [?x ?size]
             :where [[?x :shirt/size ?size]]
             :order-by [[?size :desc]]}
           (dtl/apply-options query
                              {:order-by [[:shirt/size :desc]]}))
        "An explicit direction is copied")
    (is (= '{:find [?x ?size ?weight]
             :where [[?x :shirt/size ?size]
                     [?x :shirt/weight ?weight]]
             :order-by [[?size :asc]
                        [?weight :desc]]}
           (dtl/apply-options query
                              {:order-by [:shirt/size [:shirt/weight :desc]]}))
        "Multiple fields are handled appropriately")))
