(ns multi-money.db.datomic.types
  (:require [java-time.api :as t])
  (:import [java.time LocalDate ZoneOffset]))

(defmulti coerce-id type)
(defmethod coerce-id :default [id] id)
(defmethod coerce-id String
  [id]
  (Long/parseLong id))

(defmulti ->storable type)
(defmethod ->storable :default [x] x)
(defmethod ->storable LocalDate [d]
  (t/java-date
    (t/offset-date-time
      (t/local-date-time d (t/local-time 0 0 0 0))
      ZoneOffset/UTC)))
