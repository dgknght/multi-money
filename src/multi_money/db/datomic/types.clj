(ns multi-money.db.datomic.types
  (:require [java-time.api :as t])
  (:import [java.time LocalDate ZoneOffset]))

(derive java.lang.Integer ::integer)
(derive java.lang.Long ::integer)
(derive java.lang.String ::string)
(derive clojure.lang.PersistentVector ::vector)

(defmulti coerce-id type)

(defmethod coerce-id ::integer [id] id)

(defmethod coerce-id ::string
  [id]
  (Long/parseLong id))

(defmethod coerce-id ::vector
  [v]
  (mapv (fn [x]
          (if (string? x)
            (coerce-id x)
            x))
        v))

(defmulti ->storable type)
(defmethod ->storable :default [x] x)
(defmethod ->storable LocalDate [d]
  (t/java-date
    (t/offset-date-time
      (t/local-date-time d (t/local-time 0 0 0 0))
      ZoneOffset/UTC)))
