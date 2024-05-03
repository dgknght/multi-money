(ns multi-money.db.datomic.types-test
  (:require [clojure.test :refer [deftest is]]
            [java-time.api :as t]
            [multi-money.db.datomic.types :as types]))

(deftest coerce-an-id-value
  (is (= 1 (types/coerce-id "1"))
      "A String is parsed as an integer"))

(deftest covert-a-local-date-into-a-storable-format
  (is (= #inst "2020-03-02T00:00:00.000Z"
         (types/->storable (t/local-date 2020 3 2)))
      "A LocalDate is converted to a java Date at UTC midnight"))
