(ns multi-money.db.sql.entities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [apply-to-criteria]]
            [multi-money.db :as db]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :entity [_]
  [:id :name :owner-id :default-commodity-id])

(declare ->sql-refs)
(sql/def->sql-refs ->sql-refs :entity/owner :entity/default-commodity)

(defmethod sql/before-save :entity
  [entity]
  (->sql-refs entity))

(declare ->model-refs)
(db/def->model-refs ->model-refs :entity/owner :entity/default-commodity)

(defmethod sql/after-read :entity
  [entity]
  (->model-refs entity))

(defmethod sql/prepare-criteria :entity
  [criteria]
  (apply-to-criteria criteria ->sql-refs))
