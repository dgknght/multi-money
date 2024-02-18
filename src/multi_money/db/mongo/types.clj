(ns multi-money.db.mongo.types
  (:import org.bson.types.ObjectId
           #_java.time.LocalDate
           #_java.util.Date
           #_org.bson.types.Decimal128
           #_com.fasterxml.jackson.core.JsonGenerator))

(defmulti coerce-id type)
(defmethod coerce-id :default [id] id)
(defmethod coerce-id String
  [id]
  (ObjectId. id))

(defn safe-coerce-id
  [id]
  (when id (coerce-id id)))

