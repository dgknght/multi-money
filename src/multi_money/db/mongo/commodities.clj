(ns multi-money.db.mongo.commodities
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [multi-money.db.mongo :as m]))

(defmethod m/before-save :commodity
  [commodity]
  (m/mongoify-model-refs commodity #:commodity{:entity :entity-id}))

(defmethod m/after-read :commodity
  [commodity]
  (-> commodity
      (update-in [:commodity/type] keyword)
      (rename-keys {:commodity/entity-id :commodity/entity})))
