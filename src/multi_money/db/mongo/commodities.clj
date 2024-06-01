(ns multi-money.db.mongo.commodities
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [->id]]
            [multi-money.db.mongo :as m]))

(defn- entity->id
  [x]
  (-> x
      (update-in-if [:commodity/entity] ->id)
      (rename-keys {:commodity/entity :commodity/entity-id})))

(defmethod m/before-save :commodity
  [commodity]
  (entity->id commodity))

(defmethod m/after-read :commodity
  [commodity]
  (-> commodity
      (update-in [:commodity/type] keyword)
      (rename-keys {:commodity/entity-id :commodity/entity})))

(defmethod m/prepare-criteria :commodity
  [criteria]
  (entity->id criteria))
