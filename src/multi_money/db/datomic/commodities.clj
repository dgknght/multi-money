(ns multi-money.db.datomic.commodities
  (:require [multi-money.util :refer [apply-to-criteria]]
            [multi-money.db.datomic :as d]))

(declare ->ids)
(d/def->ids ->ids :commodity/entity)

(defmethod d/before-save :commodity
  [commodity]
  (->ids commodity))

(defmethod d/prepare-criteria :commodity
  [criteria]
  (apply-to-criteria criteria ->ids))
