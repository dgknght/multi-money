(ns multi-money.dates
  (:require [java-time.api :as t]))

(defn ->java-date
  [local-date]
  (t/java-date
    (t/zoned-date-time local-date
                       (t/local-time 0 0 0 0)
                       (t/zone-offset 0 0))))

(defn ->local-date
  [_java-date]
  (t/local-date))
