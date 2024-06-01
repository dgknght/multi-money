(ns multi-money.db.sql.queries-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.sql.queries :as qrys]))

(deftest convert-a-criteria-map-into-a-sql-query
  (is (thrown-with-msg? java.lang.IllegalArgumentException #"Unable to determine the query target"
                       (qrys/criteria->query {}))
      "An exception is thrown if the model type cannot be determined.")
  (is (= ["SELECT users.* FROM users"]
         (qrys/criteria->query {} {:target :user}))
      "If the query map is empty, a target must be specified in the options.")
  (is (= ["SELECT users.* FROM users WHERE users.first_name = ?" "John"]
         (qrys/criteria->query {:user/first-name "John"}))
      "The table is derived from keyword namespaces for map with model-specific attributes")
  (is (= ["SELECT users.* FROM users WHERE users.first_name = ? LIMIT ?" "John" 1]
         (qrys/criteria->query {:user/first-name "John"} {:limit 1}))
      "A limit can be applied optionally")
  (is (= ["SELECT users.* FROM users WHERE users.first_name = ? ORDER BY users.last_name ASC" "John"]
         (qrys/criteria->query {:user/first-name "John"}
                               {:sort [:user/last-name]}))
      "A sort order can be applied optionally"))

(deftest convert-a-criteria-map-into-a-sql-query-for-count
  (is (thrown-with-msg? java.lang.IllegalArgumentException #"Unable to determine the query target"
                       (qrys/criteria->query {} {:count true}))
      "An exception is thrown if the model type cannot be determined.")
  (is (= ["SELECT COUNT(1) AS record_count FROM users"]
         (qrys/criteria->query {} {:target :user
                                   :count true}))
      "The target must be specified for an empty criteria map")
  (is (= ["SELECT COUNT(1) AS record_count FROM users WHERE users.first_name = ?" "John"]
         (qrys/criteria->query {:user/first-name "John"}
                               {:count true}))
      "The table is derived from keyword namespaces for map with model-specific attributes"))

(deftest convert-a-criteria-map-specifying-a-relation
  (is (= ["SELECT commodities.* FROM commodities INNER JOIN entities ON entities.id = commodities.entity_id WHERE entities.owner_id = ?" 101]
         (qrys/criteria->query {:entity/owner-id 101}
                               {:target :commodity}))))
