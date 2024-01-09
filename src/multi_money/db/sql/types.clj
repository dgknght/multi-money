(ns multi-money.db.sql.types)

(defn coerce-id
  [id]
  (if (string? id)
    (Long/parseLong id)
    id))
