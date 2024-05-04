(ns multi-money.db.xtdb.xtql-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.xtdb.xtql :as ql]))

(deftest extract-table-and-attributes-from-model
  (is (= [[:put-docs
           {:into :user}
           {:xt/id 101
            :first-name "John"
            :last-name "Doe"}]]
         (ql/extract-puts [{:id 101
                            :user/first-name "John"
                            :user/last-name "Doe"}]))
      "A single model yields a single :put-docs form"))
