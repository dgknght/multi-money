(ns multi-money.db.datomic.entities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db.datomic :as d]
            [dgknght.app-lib.core :refer [update-in-if]]))

(defmethod d/before-save :entity
  [entity]
  (update-in-if entity [:entity/owner] :id))
