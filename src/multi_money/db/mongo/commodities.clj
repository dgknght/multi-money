(ns multi-money.db.mongo.commodities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db.mongo :as m]))

(declare ->mongo-refs)
(m/def->mongo-refs ->mongo-refs :commodity/entity)

(defmethod m/before-save :commodity
  [commodity]
  (->mongo-refs commodity))

(declare <-mongo-refs)
(m/def<-mongo-refs <-mongo-refs :commodity/entity)

(defmethod m/after-read :commodity
  [commodity]
  (-> commodity
      (update-in [:commodity/type] keyword)
      <-mongo-refs))
