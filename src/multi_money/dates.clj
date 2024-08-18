(ns multi-money.dates
  (:require [java-time.api :as t])
  (:import [java.util Calendar Date TimeZone]))

(defn ->java-date
  [local-date]
  (t/java-date
    (t/zoned-date-time local-date
                       (t/local-time 0 0 0 0)
                       (t/zone-offset 0 0))))

(defn ->local-date
  [^Date java-date]
  (let [cal (Calendar/getInstance (TimeZone/getTimeZone "UTC"))]
    (.setTime cal java-date)
    (t/local-date (.get cal Calendar/YEAR)
                  (inc (.get cal Calendar/MONTH))
                  (.get cal Calendar/DAY_OF_MONTH))))
