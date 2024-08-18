(ns multi-money.db.datomic.entities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [apply-to-criteria]]
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
  (apply-to-criteria criteria ->ids))
