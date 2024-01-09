(ns multi-money.db.sql.types-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.sql.types :as typs]))

(deftest coerce-an-id
  (is (= 1 (typs/coerce-id "1"))
      "A string is parsed to an integer")
  (is (= 1 (typs/coerce-id 1))
      "An integer is returned as-is"))
