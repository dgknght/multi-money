(ns multi-money.db.datomic.commodities
  (:require [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [->id]]
            [multi-money.db.datomic :as d]))

(defmethod d/before-save :commodity
  [commodity]
  (update-in-if commodity [:commodity/entity] ->id))

(defmethod d/prepare-criteria :commodity
  [criteria]
  (update-in-if criteria [:commodity/entity] ->id))
