(ns multi-money.db.datomic.entities
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [update-in-criteria]]
            [multi-money.db.datomic :as d :refer [->id]]))

(defmethod d/before-save :entity
  [entity]
  (-> entity
      (update-in-if [:entity/owner] ->id)
      (update-in-if [:entity/default-commodity] ->id)))

(defmethod d/prepare-criteria :entity
  [criteria]
  (-> criteria
      (update-in-criteria [:entity/owner] ->id)
      (update-in-criteria [:entity/owner] ->id)))
