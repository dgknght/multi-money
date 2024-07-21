(ns multi-money.db.sql.entities
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :entity [_]
  [:id :name :owner-id :default-commodity-id])

(defn- owner->id
  [x]
  (-> x
      (update-in-if [:entity/owner] sql/->id)
      (rename-keys {:entity/owner :entity/owner-id})))

(defn- default-commodity->id
  [x]
  (-> x
      (update-in-if [:entity/default-commodity] sql/->id)
      (rename-keys {:entity/default-commodity :entity/default-commodity-id})))

(defmethod sql/before-save :entity
  [entity]
  (-> entity owner->id default-commodity->id))

(defmethod sql/after-read :entity
  [entity]
  (rename-keys entity {:entity/owner-id :entity/owner
                       :entity/default-commodity-id :entity/default-commodity}))
