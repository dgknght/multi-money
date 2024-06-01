(ns multi-money.db.sql.commodities
  (:require [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [->id]]
            [multi-money.db.sql :as sql]))


(defmethod sql/attributes :commodity [_]
  [:id :entity-id :symbol :name :type])

(defn- entity->id
  [x]
  (-> x
      (update-in-if [:commodity/entity] ->id)
      (rename-keys {:commodity/entity :commodity/entity-id})))

(defmethod sql/before-save :commodity
  [commodity]
  (-> commodity
      entity->id
      (update-in [:commodity/type] name)))

(defmethod sql/after-read :commodity
  [commodity]
  (-> commodity
      (rename-keys {:commodity/entity-id :commodity/entity})
      (update-in [:commodity/type] keyword)))
