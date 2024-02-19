(ns multi-money.db.mongo.types-test
  (:require [clojure.test :refer [deftest is testing]]
            [java-time.api :as t]
            [cheshire.core :as json]
            [somnium.congomongo.coerce :refer [coerce]]
            [multi-money.db.mongo.types :as typs])
  (:import [org.bson.types ObjectId Decimal128]))

(def ^:private id-str "00000020f51bb4362eee2a4d")
(def ^:private id (ObjectId. id-str))

(deftest coerce-an-id
  (is (= id (typs/coerce-id id-str))
      "A string is converted to ObjectId")
  (is (= id (typs/coerce-id id))
      "An ObjectId is returned as is"))

(deftest encode-a-mongo-id
  (is (= "{\"id\":\"00000020f51bb4362eee2a4d\"}"
         (json/generate-string {:id id}))))

(deftest encode-a-date
  (is (= "{\"date\":\"2020-03-02\"}"
         (json/generate-string {:date (t/local-date 2020 3 2)}))))

(deftest convert-a-date
  (testing "clojure to mongo"
    (is (= #inst "2020-03-02T00:00:00Z"
           (coerce (t/local-date 2020 3 2) [:clojure :mongo]))))
  (testing "mongo to clojure"
    (is (= (t/local-date 2020 3 2)
           (coerce #inst "2020-03-02T00:00:00Z" [:mongo :clojure])))))

(deftest convert-a-money-value
  (testing "clojure to mongo"
    (is (= (Decimal128/parse "12.34")
           (coerce 12.34M [:clojure :mongo]))))
  (testing "mongo to clojure"
    (is (= 12.34M
           (coerce (Decimal128/parse "12.34") [:mongo :clojure])))))
