(ns multi-money.dates-test
  (:require [clojure.test :refer [deftest is]]
            [java-time.api :as t]
            [multi-money.dates :as d])
  (:import [java.util Calendar TimeZone]))

(deftest convert-a-local-date-to-a-java-date
  (is (= (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
           (.set cal 2020 2 2 0 0 0) ; January is 0
           (.getTime cal))
         (d/->java-date (t/local-date 2020 3 2)))))
