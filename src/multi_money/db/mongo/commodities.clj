(ns multi-money.db.mongo.commodities
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [->id]]
            [multi-money.db.mongo :as m]
            [multi-money.db.mongo.types :refer [coerce-id]]))

(defn- entity->id
  [x]
  (-> x
      (update-in-if [:commodity/entity] (comp coerce-id ->id))
      (rename-keys {:commodity/entity :commodity/entity-id})))

(defmethod m/before-save :commodity
  [commodity]
  (entity->id commodity))

(defmethod m/after-read :commodity
  [commodity]
  (-> commodity
      (update-in [:commodity/type] keyword)
      (rename-keys {:commodity/entity-id :commodity/entity})))
