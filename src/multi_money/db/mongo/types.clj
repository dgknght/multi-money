(ns multi-money.db.mongo.types
  (:require [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [somnium.congomongo.coerce :refer [ConvertibleFromMongo
                                               ConvertibleToMongo]]
            [cheshire.generate :refer [add-encoder]])
  (:import [org.bson.types ObjectId Decimal128]
           java.util.Date
           java.math.BigDecimal
           [java.time LocalDate ZoneId ZoneOffset]
           com.fasterxml.jackson.core.JsonGenerator))

(derive ObjectId ::object-id)
(derive java.lang.String ::string)
(derive clojure.lang.PersistentVector ::vector)

(add-encoder ObjectId
             (fn [^ObjectId id ^JsonGenerator g]
               (.writeString g (str id))))

; This is actually not specified to MongoDB
(add-encoder LocalDate
             (fn [^LocalDate d ^JsonGenerator g]
               (.writeString g (str d))))

(extend-protocol ConvertibleToMongo
  LocalDate
  (clojure->mongo [^LocalDate d]
    (t/java-date
      (.toInstant (.atStartOfDay d)
                  ZoneOffset/UTC)))
  BigDecimal
  (clojure->mongo [^BigDecimal d]
    (Decimal128. d)))

(extend-protocol ConvertibleFromMongo
  Date ; TODO: Will we ever want this to be a date-time?
  (mongo->clojure [^Date d _kwd]
    (.toLocalDate
      (.atZone (t/instant d)
               (ZoneId/systemDefault))))

  Decimal128
  (mongo->clojure [^Decimal128 d _kwd] (.bigDecimalValue d)))

(defmulti coerce-id type)

(defmethod coerce-id ::object-id [id] id)

(defmethod coerce-id ::string
  [id]
  (ObjectId. id))

(defmethod coerce-id ::vector
  [v]
  (mapv (fn [x]
          (if (string? x)
            (coerce-id x)
            x))
        v))
