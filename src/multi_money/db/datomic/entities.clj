(ns multi-money.db.datomic.entities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [update-in-criteria]]
            [multi-money.db.datomic :as d :refer [->id]]))

(defn- adjust-ids
  [m]
  (-> m
      (update-in-criteria [:entity/owner] ->id)
      (update-in-criteria [:entity/default-commodity] ->id)))

(defmethod d/before-save :entity
  [entity]
  (adjust-ids entity))

(defmethod d/prepare-criteria :entity
  [criteria]
  (adjust-ids criteria))
