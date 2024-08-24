(ns multi-money.db.datomic.commodities
  (:require [stowaway.criteria :as crt]
            [multi-money.db.datomic :as d]))

(declare ->ids)
(d/def->ids ->ids :commodity/entity)

(defmethod d/before-save :commodity
  [commodity]
  (->ids commodity))

(defmethod d/prepare-criteria :commodity
  [criteria]
  (crt/apply-to criteria ->ids))
