(ns multi-money.db.mongo.queries-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.mongo.queries :as q]))

(deftest convert-criteria-into-a-query
  (is (= {:where {:name "John"}}
         (q/criteria->query {:name "John"}))
      "A simple attribute equalify criterion is left as-is"))
