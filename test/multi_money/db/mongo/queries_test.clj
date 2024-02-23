(ns multi-money.db.mongo.queries-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.mongo.queries :as q]))

(deftest convert-simple-criteria-into-a-query
  (is (= {:where {:name "John"}}
         (q/criteria->query {:name "John"}))
      "A simple attribute equalify criterion is left as-is")
  (is (= {:where {:first_name "John"}}
         (q/criteria->query {:first-name "John"}))
      "A kebab-case key is converted to snake case"))

(deftest convert-criterion-with-comparison
  (is (= {:where {:avg_size {:$gt 5}}}
         (q/criteria->query {:avg-size [:> 5]}))
      ":> translates to :$gt")
  (is (= {:where {:avg_size {:$gte 5}}}
         (q/criteria->query {:avg-size [:>= 5]}))
      ":>= translates to :$gte")
  (is (= {:where {:avg_size {:$lt 5}}}
         (q/criteria->query {:avg-size [:< 5]}))
      ":< translates to :$lt")
  (is (= {:where {:avg_size {:$lte 5}}}
         (q/criteria->query {:avg-size [:<= 5]}))
      ":<= translates to :$lte"))

(deftest convert-compound-criterion
  (is (= {:where {:$or [{:first_name "John"}
                        {:last_name "Doe"}]}}
         (q/criteria->query [:or
                             {:first-name "John"}
                             {:last-name "Doe"}]))
      "A top-level :or is convered correctly")
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo #"(?i)unsupported"
        (q/criteria->query [:and
                            {:first-name "John"}
                            {:last-name "Doe"}]))
      "$and is not supported (map is preferred)")
  (is (= {:where {:size {:$gte 2 :$lt 5}}}
         (q/criteria->query {:size [:and [:>= 2] [:< 5]]}))))

(deftest apply-a-sort
  (is (= {:sort {"first_name" 1}}
         (q/criteria->query {} {:sort [[:first-name :asc]]}))
      ":sort is translated")
  (is (= {:sort {"first_name" 1}}
         (q/criteria->query {} {:order-by [[:first-name :asc]]}))
      ":order-by is is translated as :sort")
  (is (= {:sort {"first_name" -1}}
         (q/criteria->query {} {:order-by [[:first-name :desc]]}))
      "Order can be descending")
  (is (= {:sort {"first_name" 1}}
         (q/criteria->query {} {:order-by [:first-name]}))
      "Order is ascending by default"))
