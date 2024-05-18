(ns multi-money.db.sql.types)

(derive java.lang.Integer ::integer)
(derive java.lang.Long ::integer)
(derive java.lang.String ::string)
(derive clojure.lang.PersistentVector ::vector)

(defmulti coerce-id type)

(defmethod coerce-id ::string
  [s]
  (Long/parseLong s))

(defmethod coerce-id ::integer
  [id]
  id)

(defmethod coerce-id ::vector
  [v]
  (mapv (fn [x]
          (if (string? x)
            (coerce-id x)
            x))
        v))
