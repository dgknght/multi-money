(ns multi-money.db.mongo.entities
  (:require [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [->id]]
            [multi-money.db.mongo :as m]))

(defn- owner->id
  [x]
  (-> x
      (update-in-if [:entity/owner] ->id)
      (rename-keys {:entity/owner :entity/owner-id})))

(defmethod m/before-save :entity
  [entity]
  (owner->id entity))

(defmethod m/after-read :entity
  [entity]
  (rename-keys entity {:entity/owner-id :entity/owner}))

(defmethod m/prepare-criteria :entity
  [criteria]
  (owner->id criteria))
