(ns multi-money.accounts-test
  (:require [clojure.test :refer [deftest is]]
            [dgknght.app-lib.test-assertions]
            [multi-money.accounts :as a]))

(deftest identity-a-left-side-account
  (is (a/left-side? {:account/type :asset})
      "An asset account is \"left side\"")
  (is (a/left-side? {:account/type :expense})
      "An expense account is \"left side\"")
  (is (not (a/left-side? {:account/type :liability}))
      "A liability account is not \"left side\"")
  (is (not (a/left-side? {:account/type :equity}))
      "An equity account is not \"left side\"")
  (is (not  (a/left-side? {:account/type :income}))
      "An income account is not \"left side\""))

(def ^:private flat-accounts
  [{:id 101
    :account/name "Savings"
    :account/type :asset}
   {:id 102
    :account/name "Car"
    :account/type :asset
    :account/parent {:id 101}}
   {:id 103
    :account/name "College"
    :account/type :asset
    :account/parent {:id 101}}])

(def ^:private nested-accounts
  [{:id 101
    :account/name "Savings"
    :account/type :asset
    :path ["Savings"]
    :depth 0
    :children [{:id 102
                :account/name "Car"
                :account/type :asset
                :account/parent {:id 101
                                 :account/name "Savings"
                                 :account/type :asset
                                 :path ["Savings"]}
                :depth 1
                :path ["Savings" "Car"]
                :children []}
               {:id 103
                :account/name "College"
                :account/type :asset
                :account/parent {:id 101
                                 :account/name "Savings"
                                 :account/type :asset
                                 :path ["Savings"]}
                :depth 1
                :path ["Savings" "College"]
                :children []}]}])

(deftest nest-account-children
  (is (= nested-accounts
         (a/nest flat-accounts))))

(def ^:private annotations
  [{:depth 0
    :path ["Savings"]
    :children [{:id 102
                :account/name "Car"
                :account/type :asset
                :account/parent {:id 101
                                 :account/name "Savings"
                                 :account/type :asset
                                 :path ["Savings"]}
                :depth 1
                :path ["Savings" "Car"]
                :children []}
               {:id 103
                :account/name "College"
                :account/type :asset
                :account/parent {:id 101
                         :account/name "Savings"
                         :account/type :asset
                         :path ["Savings"]}
                :depth 1
                :path ["Savings" "College"]
                :children []}]}
   {:depth 1
    :path ["Savings" "Car"]
    :children []
    :account/parent {:id 101
                     :account/name "Savings"
                     :account/type :asset
                     :path ["Savings"]}}
   {:depth 1
    :path ["Savings" "College"]
    :children []
    :account/parent {:id 101
                     :account/name "Savings"
                     :account/type :asset
                     :path ["Savings"]}}])

(def ^:private annotated-flat-accounts
  (->> annotations
       (interleave flat-accounts)
       (partition 2)
       (map #(apply merge %))))

(deftest unnest-account-children
  (is (= annotated-flat-accounts
         (a/unnest nested-accounts))))
