(ns multi-money.db.sql.queries-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.sql.queries :as qrys]))

(deftest convert-a-criteria-map-into-a-sql-query
  (is (thrown-with-msg? java.lang.AssertionError #"^Must be able to determine the model type"
                       (qrys/criteria->query {}))
      "An exception is thrown if the model type cannot be determined.")
  (is (= ["SELECT * FROM users"]
         (qrys/criteria->query ^{:model-type :user} {}))
      "The table is derived from meta data for an empty map")
  (is (= ["SELECT * FROM users WHERE users.id = ?" 101]
         (qrys/criteria->query ^{:model-type :user} {:id 101}))
      "The table is derived from meta data for map with only and :id attributes")
  (is (= ["SELECT * FROM users WHERE users.first_name = ?" "John"]
         (qrys/criteria->query {:user/first-name "John"}))
      "The table is derived from keyword namespaces for map with model-specific attributes")
  (is (= ["SELECT * FROM users WHERE users.first_name = ? LIMIT ?" "John" 1]
         (qrys/criteria->query {:user/first-name "John"} {:limit 1}))
      "A limit can be applied optionally")
  (is (= ["SELECT * FROM users WHERE users.first_name = ? ORDER BY last_name ASC" "John"]
         (qrys/criteria->query {:user/first-name "John"} {:order-by [:last-name]}))
      "A sort order can be applied optionally"))
