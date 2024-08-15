(ns multi-money.db.mongo.entities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [apply-to-criteria]]
            [multi-money.db.mongo :as m]))

(declare ->mongo-refs)
(m/def->mongo-refs ->mongo-refs :entity/owner :entity/default-commodity)


(declare <-mongo-refs)
(m/def<-mongo-refs <-mongo-refs :entity/owner :entity/default-commodity)

(defmethod m/before-save :entity
  [entity]
  (->mongo-refs entity))

(defmethod m/after-read :entity
  [entity]
  (<-mongo-refs entity))

(defmethod m/prepare-criteria :entity
  [criteria]
  (apply-to-criteria criteria ->mongo-refs))
