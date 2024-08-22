(ns multi-money.db.sql.partitioning-test
  (:require [clojure.test :refer [deftest testing is]]
            [next.jdbc :as jdbc]
            [java-time.api :as t]
            [multi-money.db.sql.partitioning :as prt]))

(defmacro ^:private with-cmd-intercept
  [& body]
  `(let [cmds# (atom #{})]
     (with-redefs [jdbc/execute! (fn [_conn# cmd#]
                                   (swap! cmds# conj cmd#))]
       ~@body)
     @cmds#))

(deftest partition-by-year
  ; NB: The lower bound of the range is inclusive and the upper bound is exclusive
  (testing "start on an anchor year"
    (let [expected #{#_"CREATE TABLE IF NOT EXISTS prices_y2001 PARTITION OF prices FOR VALUES FROM ('2001-01-01') TO ('2002-01-01');"
                    ["CREATE TABLE IF NOT EXISTS transactions_y2001 PARTITION OF transactions FOR VALUES FROM ('2001-01-01') TO ('2002-01-01');"]
                    ["CREATE TABLE IF NOT EXISTS transaction_items_y2001 PARTITION OF transaction_items FOR VALUES FROM ('2001-01-01') TO ('2002-01-01');"]
                     #_"CREATE TABLE IF NOT EXISTS reconciliations_y2001_y2005 PARTITION OF reconciliations FOR VALUES FROM ('2001-01-01') TO ('2006-01-01');"
                     #_"CREATE TABLE IF NOT EXISTS cached_prices_y2001 PARTITION OF cached_prices FOR VALUES FROM ('2001-01-01') TO ('2002-01-01');"}
          cmds (with-cmd-intercept
                 (prt/create-partition-tables
                  (t/local-date 2001 1 1)
                  (t/local-date 2001 12 31)
                  {:silent true
                   :intervals {:default {:interval-type :year
                                         :interval-count 1}}}))]
      (is (= expected cmds))))
  (testing "start on a non-anchor year"
    (let [expected #{#_"CREATE TABLE IF NOT EXISTS prices_y2002 PARTITION OF prices FOR VALUES FROM ('2002-01-01') TO ('2003-01-01');"
                    ["CREATE TABLE IF NOT EXISTS transactions_y2002 PARTITION OF transactions FOR VALUES FROM ('2002-01-01') TO ('2003-01-01');"]
                    ["CREATE TABLE IF NOT EXISTS transaction_items_y2002 PARTITION OF transaction_items FOR VALUES FROM ('2002-01-01') TO ('2003-01-01');"]
                     #_"CREATE TABLE IF NOT EXISTS reconciliations_y2001_y2005 PARTITION OF reconciliations FOR VALUES FROM ('2001-01-01') TO ('2006-01-01');"
                     #_"CREATE TABLE IF NOT EXISTS cached_prices_y2002 PARTITION OF cached_prices FOR VALUES FROM ('2002-01-01') TO ('2003-01-01');" }
          cmds (with-cmd-intercept
                 (prt/create-partition-tables
                  (t/local-date 2002 1 1)
                  (t/local-date 2002 12 31)
                  {:silent true
                   :intervals {:default {:interval-type :year
                                         :interval-count 1}}}))]
      (is (= expected cmds)))))

(deftest partition-by-month
  (testing "start on an anchor month"
    (let [expected #{#_"CREATE TABLE IF NOT EXISTS prices_y2020_m01 PARTITION OF prices FOR VALUES FROM ('2020-01-01') TO ('2020-02-01');"
                     #_"CREATE TABLE IF NOT EXISTS prices_y2020_m02 PARTITION OF prices FOR VALUES FROM ('2020-02-01') TO ('2020-03-01');"
                     ["CREATE TABLE IF NOT EXISTS transactions_y2020_m01 PARTITION OF transactions FOR VALUES FROM ('2020-01-01') TO ('2020-02-01');"]
                     ["CREATE TABLE IF NOT EXISTS transactions_y2020_m02 PARTITION OF transactions FOR VALUES FROM ('2020-02-01') TO ('2020-03-01');"]
                     ["CREATE TABLE IF NOT EXISTS transaction_items_y2020_m01 PARTITION OF transaction_items FOR VALUES FROM ('2020-01-01') TO ('2020-02-01');"]
                     ["CREATE TABLE IF NOT EXISTS transaction_items_y2020_m02 PARTITION OF transaction_items FOR VALUES FROM ('2020-02-01') TO ('2020-03-01');"]
                     #_"CREATE TABLE IF NOT EXISTS reconciliations_y2020_m01 PARTITION OF reconciliations FOR VALUES FROM ('2020-01-01') TO ('2020-02-01');"
                     #_"CREATE TABLE IF NOT EXISTS reconciliations_y2020_m02 PARTITION OF reconciliations FOR VALUES FROM ('2020-02-01') TO ('2020-03-01');"
                     #_"CREATE TABLE IF NOT EXISTS cached_prices_y2020_m01 PARTITION OF cached_prices FOR VALUES FROM ('2020-01-01') TO ('2020-02-01');"
                     #_"CREATE TABLE IF NOT EXISTS cached_prices_y2020_m02 PARTITION OF cached_prices FOR VALUES FROM ('2020-02-01') TO ('2020-03-01');" }
          cmds (with-cmd-intercept
                 (prt/create-partition-tables
                  (t/local-date 2020 1 1)
                  (t/local-date 2020 2 29)
                  {:silent true
                   :rules {:prices            {:interval-type :month
                                               :interval-count 1}
                           :cached_prices     {:interval-type :month
                                               :interval-count 1}
                           :transactions      {:interval-type :month
                                               :interval-count 1}
                           :transaction_items {:interval-type :month
                                               :interval-count 1}
                           :reconciliations   {:interval-type :month
                                               :interval-count 1}}}))]
      (is (= expected cmds))))
  (testing "start on a non-anchor month"
    (let [expected #{#_"CREATE TABLE IF NOT EXISTS prices_y2020_m01_m02 PARTITION OF prices FOR VALUES FROM ('2020-01-01') TO ('2020-03-01');"
                     #_"CREATE TABLE IF NOT EXISTS prices_y2020_m03_m04 PARTITION OF prices FOR VALUES FROM ('2020-03-01') TO ('2020-05-01');"
                     ["CREATE TABLE IF NOT EXISTS transactions_y2020_m01_m02 PARTITION OF transactions FOR VALUES FROM ('2020-01-01') TO ('2020-03-01');"]
                     ["CREATE TABLE IF NOT EXISTS transactions_y2020_m03_m04 PARTITION OF transactions FOR VALUES FROM ('2020-03-01') TO ('2020-05-01');"]
                     ["CREATE TABLE IF NOT EXISTS transaction_items_y2020_m01_m02 PARTITION OF transaction_items FOR VALUES FROM ('2020-01-01') TO ('2020-03-01');"]
                     ["CREATE TABLE IF NOT EXISTS transaction_items_y2020_m03_m04 PARTITION OF transaction_items FOR VALUES FROM ('2020-03-01') TO ('2020-05-01');"]
                     #_"CREATE TABLE IF NOT EXISTS reconciliations_y2020_m01_m02 PARTITION OF reconciliations FOR VALUES FROM ('2020-01-01') TO ('2020-03-01');"
                     #_"CREATE TABLE IF NOT EXISTS reconciliations_y2020_m03_m04 PARTITION OF reconciliations FOR VALUES FROM ('2020-03-01') TO ('2020-05-01');"
                     #_"CREATE TABLE IF NOT EXISTS cached_prices_y2020_m01_m02 PARTITION OF cached_prices FOR VALUES FROM ('2020-01-01') TO ('2020-03-01');"
                     #_"CREATE TABLE IF NOT EXISTS cached_prices_y2020_m03_m04 PARTITION OF cached_prices FOR VALUES FROM ('2020-03-01') TO ('2020-05-01');"}
          cmds (with-cmd-intercept
                 (prt/create-partition-tables
                   (t/local-date 2020 2 1)
                   (t/local-date 2020 3 31)
                   {:silent true
                    :rules {:prices            {:interval-type :month
                                                :interval-count 2}
                            :cached_prices     {:interval-type :month
                                                :interval-count 2}
                            :transactions      {:interval-type :month
                                                :interval-count 2}
                            :transaction_items {:interval-type :month
                                                :interval-count 2}
                            :reconciliations   {:interval-type :month
                                                :interval-count 2}}}))]
      (is (= expected cmds)))))
