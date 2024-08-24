(ns multi-money.db.datomic.entities
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.criteria :as crt]
            [multi-money.db.datomic :as d]))

(declare ->ids)
(d/def->ids ->ids
  :entity/owner
  :entity/default-commodity)

(defmethod d/before-save :entity
  [entity]
  (->ids entity))

(defmethod d/prepare-criteria :entity
  [criteria]
  (crt/apply-to criteria ->ids))
