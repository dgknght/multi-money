(ns multi-money.db.sql.types)

(derive java.lang.Integer ::integer)
(derive java.lang.Long ::integer)
(derive java.lang.String ::string)
(derive java.util.UUID ::uuid)
(derive clojure.lang.PersistentVector ::vector)
(derive ::integer ::id)
(derive ::uuid ::id)

(defmulti coerce-id type)

(defmethod coerce-id ::id [id] id)

(defmethod coerce-id ::string
  [s]
  (Long/parseLong s))

(defmethod coerce-id ::vector
  [v]
  (mapv (fn [x]
          (if (string? x)
            (coerce-id x)
            x))
        v))
