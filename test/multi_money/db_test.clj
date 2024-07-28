(ns multi-money.db-test
  (:require [clojure.test :refer [deftest are]]
            [multi-money.db :as db]))

(deftest identity-a-simple-model-ref
  (are [input expected] (= expected (db/simple-model-ref? input))
       {:id 1}                    true
       {:id 1 :first-name "John"} false
       1                          false))
