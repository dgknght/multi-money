(ns multi-money.db.sql.types)

(defmulti coerce-id type)

(defmethod coerce-id String
  [s]
  (Long/parseLong s))

(defmethod coerce-id java.lang.Integer
  [id]
  id)

(defmethod coerce-id clojure.lang.PersistentVector
  [v]
  (mapv (fn [x]
          (if (string? x)
            (coerce-id x)
            x))
        v))
