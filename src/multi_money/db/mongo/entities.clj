(ns multi-money.db.mongo.entities
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.criteria :as crt]
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
  (crt/apply-to criteria ->mongo-refs))
