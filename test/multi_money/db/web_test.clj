(ns multi-money.db.web-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.pprint :refer [pprint]]
            [multi-money.db :as db]
            [multi-money.db.web :as w]
            [clojure.core :as c]))

(defn- test-request
  [req]
  (with-redefs [db/reify-storage (fn [_cfg]
                                   (reify db/Storage
                                     (put [_ _])
                                     (select [_ _ _])
                                     (delete [_ _])
                                     (close [_])
                                     (reset [_])))]
    (let [calls (atom [])
          handler (w/wrap-db
                    (fn [req]
                      (swap! calls conj req)
                      {:body "OK"}))
          res (handler req)]
      {:calls @calls
       :res res})))

(deftest wrap-a-request-with-a-db
  (testing "cookies"
    (let [res (test-request {:cookies {"db-strategy" {:value "sql"}}})]
      (is (= :sql (:db-strategy (first (:calls res)))))))
  (testing "header"
    (let [res (test-request {:headers {"db-strategy" "mongo"}})]
      (is (= :mongo (:db-strategy (first (:calls res)))))))
  (testing "fallback"
    (let [res (test-request {})]
      (is (= :sql (:db-strategy (first (:calls res))))))))
