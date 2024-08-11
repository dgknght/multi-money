(ns multi-money.db.sql.commodities
  (:require [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :commodity [_]
  [:id :entity-id :symbol :name :type])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs :commodity/entity)

(defmethod sql/before-save :commodity
  [commodity]
  (-> commodity
      ->sql-refs
      (update-in [:commodity/type] name)))

(declare ->model-refs)
(db/def->model-refs ->model-refs :commodity/entity)

(defmethod sql/after-read :commodity
  [commodity]
  (-> commodity
      ->model-refs
      (update-in [:commodity/type] keyword)))
