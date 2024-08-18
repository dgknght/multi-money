(ns multi-money.dates-test
  (:require [clojure.test :refer [deftest is]]
            [java-time.api :as t]
            [multi-money.dates :as d])
  (:import [java.util Calendar TimeZone]))

(def java-date (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
                 (.clear cal)
                 (.set cal 2020 2 2) ; January is 0
                 (.getTime cal)))

(deftest convert-a-local-date-to-a-java-date
  (is (= java-date
         (d/->java-date (t/local-date 2020 3 2)))))

(deftest convert-a-java-date-to-a-local-date
  (is (= (t/local-date 2020 3 2)
         (d/->local-date java-date))))
