(ns multi-money.util-test
  (:require [clojure.test :refer [deftest testing is are]]
            #?(:clj [java-time.api :as t]
               :cljs [cljs-time.core :as t])
            [dgknght.app-lib.test-assertions]
            [multi-money.util :as utl])
  (:import java.lang.AssertionError))

(def stored-date
  #?(:clj 18262
     :cljs 1577836800000))
(deftest convert-a-date-to-a-storable-value
  (is (= stored-date
         (utl/->storable-date (t/local-date 2020 1 1)))))

(deftest convert-a-stored-value-to-local-date
  (is (t/= (t/local-date 2020 1 1)
         (utl/<-storable-date stored-date))))

(deftest qualify-map-keys
  (is (= {:entity/name "Personal"}
         (utl/qualify-keys {:name "Personal"} :entity))
      "Unqualified keys are qualified with the model type")
  (is (= {:db/id "x"}
         (utl/qualify-keys {:db/id "x"} :entity))
      "Qualified keys are left as-is")
  (is (= {:id 101
          :user/name "John"}
         (utl/qualify-keys {:id 101 :name "John"}
                           :user
                           :ignore #{:id}))
      "Keys can be explicitly ignored"))

(deftest unqaulify-map-keys
  (is (= {:name "Personal"}
         (utl/unqualify-keys {:entity/name "Personal"}))))

(deftest extract-a-qualifier
  (is (= "user" (utl/qualifier {:user/name "John"}))
      "A single qualifier is taken directly")
  (is (= "user" (utl/qualifier {:id 101
                                :user/name "John"}))
      "Nil namespaces are ignored")
  (is (thrown-with-msg?
        AssertionError
        #"more than one keyword namespace"
        (utl/qualifier {:user/name "John"
                        :address/line-1 "1234 Main St"}))))

(deftest prepend-a-value
  (is (= [:new :a :b]
         (utl/prepend [:a :b] :new))
      "The value is added at the front of a vector")
  (is (= '(:new :a :b)
         (utl/prepend '(:a :b) :new))
      "The value is added at the front of a list"))

(deftest apply-sort-rule
  (let [d1 (t/local-date 2001 1 1)
        d2 (t/local-date 2002 1 1)
        d3 (t/local-date 2004 1 1)
        items [{:v 2 :d d1 :s "carrot"}
               {:v 1 :d d3 :s "banana"}
               {:v 3 :d d2 :s "apple"}]]
    (testing "Ascending sort on one string field, implicit direction"
      (is (= [{:s "apple"}
              {:s "banana"}
              {:s "carrot"}]
             (map #(select-keys % [:s])
                  (utl/apply-sort {:order-by [:s]}
                                  items)))))
    (testing "Ascending sort on one integer field, implicit direction"
      (is (= items
             (utl/apply-sort {} items))))
    (testing "Ascending sort on one integer field, implicit direction"
      (is (= [{:v 1}
              {:v 2}
              {:v 3}]
             (map #(select-keys % [:v])
                  (utl/apply-sort {:order-by [:v]}
                                  items)))))
    (testing "Ascending sort on one date field, implicit direction"
      (is (= [{:d d1}
              {:d d2}
              {:d d3}]
             (map #(select-keys % [:d])
                  (utl/apply-sort {:order-by [:d]}
                                  items)))))
    (testing "Ascending sort on one integer field, explicit direction"
      (is (= [{:v 1}
              {:v 2}
              {:v 3}]
             (map #(select-keys % [:v])
                  (utl/apply-sort {:order-by [[:v :asc]]}
                                  items)))))
    (testing "Descending sort on one integer field"
      (is (= [{:v 3}
              {:v 2}
              {:v 1}]
             (map #(select-keys % [:v])
                  (utl/apply-sort {:order-by [[:v :desc]]}
                                  items)))))
    (testing "Multi-field sort"
      (is (= [{:v 1 :d d3}
              {:v 2 :d d1}
              {:v 2 :d d2}
              {:v 3 :d d2}]
             (map #(select-keys % [:v :d])
                  (utl/apply-sort {:order-by [:v :d]}
                                  (conj items {:v 2 :d d2}))))))))

(deftest separate-nils-from-a-model
  (is (= [{:present :here}
          [:absent]]
         (utl/split-nils {:present :here
                          :absent nil}))))

(deftest identify-a-scalar-value
  (are [input expected] (= expected (utl/scalar? input))
       1        true
       :keyword true
       "string" true
       {}       false
       []       false
       '()      false
       #{}      false))

(deftest identify-a-valid-id-value
  (are [input expected] (= expected (utl/valid-id? input))
       1             true
       ;(random-uuid) true
       "mything"     true
       :mything      true
       {:id 1}       false
       [1]           false
       #{1}          false))

(deftest truncate-a-string
  (are [input length expected] (= expected
                                  (utl/truncate input {:length length}))
       "Business Datomic" 12 "Business D"
       "Business Datomic" 16 "Business Datomic"
       nil                12 nil))

(deftest truncate-string-for-html
  (are [input length expected] (= expected
                                  (utl/truncate-html input {:length length}))
       "Business Datomic" 12 [:span {:title "Business Datomic"} "Business D"]
       "Business Datomic" 16 [:span {:title "Business Datomic"} "Business Datomic"]
       nil                12 nil))

(deftest update-in-criteria
  (is (= {:count 2}
         (utl/update-in-criteria {:count 1} [:count] inc))
      "A value present in a map is updated")
  (is (= [:or
          {:count 3}
          {:size :large}]
         (utl/update-in-criteria [:or {:count 1} {:size :large}]
                                 [:count]
                                 + 2))
      "A value in a map in a vector is updated"))

(deftest rename-keys-in-a-criteria-structure
  (is (= [:or
          {#{:debit-account-id :credit-account-id} 101}
          {:memo "this one"}]
         (utl/rename-criteria-keys [:or {:account-id 101} {:memo "this one"}]
                                   {:account-id #{:debit-account-id :credit-account-id}}))))

(deftest extract-a-model-id
  (is (= 101 (utl/->id 101))
      "A scalar value is returned")
  (is (= 101 (utl/->id {:id 101}))
      "The value at :id is returned from map")
  (is (= 101 (utl/->id {:user/id 101}))
      "The value at a namespaced key with name \"id\" is returned"))
