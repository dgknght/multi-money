(ns multi-money.db.mongo.entities
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.db.mongo :as m]))

(defn- mongoify-model-refs
  [m]
  (m/mongoify-model-refs m {:entity/owner :entity/owner-id
                            :entity/default-commodity :entity/default-commodity-id}))

(defn- generalize-model-refs
  [entity]
  (-> entity
      (rename-keys {:entity/owner-id :entity/owner
                    :entity/default-commodity-id :entity/default-commodity})
      (update-in [:entity/owner] #(hash-map :id %))
      (update-in-if [:entity/default-commodity] #(hash-map :id %))))

(defmethod m/before-save :entity
  [entity]
  (mongoify-model-refs entity))

(defmethod m/after-read :entity
  [entity]
  (generalize-model-refs entity))

(defmethod m/prepare-criteria :entity
  [criteria]
  (mongoify-model-refs criteria))
