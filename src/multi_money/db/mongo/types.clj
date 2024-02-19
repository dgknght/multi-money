(ns multi-money.db.mongo.types
  (:import org.bson.types.ObjectId
           #_java.time.LocalDate
           #_java.util.Date
           #_org.bson.types.Decimal128
           #_com.fasterxml.jackson.core.JsonGenerator))

#_(add-encoder ObjectId
             (fn [^ObjectId id ^JsonGenerator g]
               (.writeString g (str id))))

#_(extend-protocol ConvertibleToMongo
  LocalDate
  (clojure->mongo [^LocalDate d] (t/java-date d)))

#_(extend-protocol ConvertibleFromMongo
  Date
  (mongo->clojure [^Date d _kwd] (t/local-date d))

  Decimal128
  (mongo->clojure [^Decimal128 d _kwd] (.bigDecimalValue d)))

(defmulti coerce-id type)
(defmethod coerce-id :default [id] id)
(defmethod coerce-id String
  [id]
  (ObjectId. id))
