(ns multi-money.models.commodities-test
  (:require [clojure.test :refer [is use-fixtures]]
            [dgknght.app-lib.test-assertions]
            [multi-money.test-context :refer [with-context
                                             find-entity]]
            [multi-money.helpers :refer [reset-db
                                        dbtest]]
            [multi-money.models.commodities :as cdts]
            [multi-money.models.mongodb.ref]
            [multi-money.models.sql.ref]
            [multi-money.models.xtdb.ref]
            [multi-money.models.datomic.ref]))

(use-fixtures :each reset-db)

(def ^:private create-ctx
  {:entities [{:name "Personal"}
              {:name "Business"}]})

(dbtest create-a-commodity
        (with-context create-ctx
          (let [entity (find-entity "Personal")
                commodity {:entity-id (:id entity)
                           :name "United States Dollar"
                           :symbol "USD"
                           :type :currency}]
            (cdts/put commodity)
            (is (seq-of-maps-like? [commodity]
                                   (cdts/select {:entity-id (:id entity)}))
                "A saved commodity can be retrieved"))))

(def ^:private find-ctx
  (assoc create-ctx
         :commodities [{:entity-id "Personal"
                        :name "United States Dollar"
                        :symbol "USD"
                        :type :currency}
                       {:entity-id "Personal"
                        :name "British Pound"
                        :symbol "GBP"
                        :type :currency}
                       {:entity-id "Business"
                        :name "Euro"
                        :symbol "EUR"
                        :type :currency}]))

(dbtest find-by-entity
  (with-context find-ctx
    (is (= #{{:name "United States Dollar"
              :symbol "USD"
              :type :currency}
             {:name "British Pound"
              :symbol "GBP"
              :type :currency}}
           (->> (cdts/select {:entity-id (:id (find-entity "Personal"))})
                (map #(select-keys % [:name :type :symbol]))
                (into #{})))
        "The commodities for the specified entity are returned")))
