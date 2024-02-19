(ns multi-money.db.mongo.types
  (:require [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [somnium.congomongo.coerce :refer [ConvertibleFromMongo
                                               ConvertibleToMongo]]
            [cheshire.generate :refer [add-encoder]])
  (:import [org.bson.types ObjectId Decimal128]
           java.util.Date
           [java.time LocalDate ZoneOffset]
           com.fasterxml.jackson.core.JsonGenerator))

(add-encoder ObjectId
             (fn [^ObjectId id ^JsonGenerator g]
               (.writeString g (str id))))

(add-encoder LocalDate
             (fn [^LocalDate d ^JsonGenerator g]
               (.writeString g (str d))))

(extend-protocol ConvertibleToMongo
  LocalDate
  (clojure->mongo [^LocalDate d] (t/java-date (.toInstant (.atStartOfDay d)
                                                          (ZoneOffset/UTC)))))

(extend-protocol ConvertibleFromMongo
  Date ; TODO: Will we ever want this to be a date-time?
  (mongo->clojure [^Date d _kwd]
    (let [f (t/formatter "yyyy-MM-dd")]
      (t/local-date f (t/format f d))))

  Decimal128
  (mongo->clojure [^Decimal128 d _kwd] (.bigDecimalValue d)))

(defmulti coerce-id type)
(defmethod coerce-id :default [id] id)
(defmethod coerce-id String
  [id]
  (ObjectId. id))
