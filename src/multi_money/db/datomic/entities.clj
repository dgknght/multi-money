(ns multi-money.db.datomic.entities
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.db.datomic :as d]
            [multi-money.util :refer [->id]]))

(defmethod d/before-save :entity
  [entity]
  (update-in-if entity [:entity/owner] ->id))
