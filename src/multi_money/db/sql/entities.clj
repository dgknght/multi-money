(ns multi-money.db.sql.entities
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :entity [_]
  [:id :name :owner-id])

(defmethod sql/before-save :entity
  [entity]
  (-> entity
      (update-in-if [:entity/owner] :id)
      (rename-keys {:entity/owner :entity/owner-id})))

(defmethod sql/after-read :entity
  [entity]
  (rename-keys entity {:entity/owner-id :entity/owner}))
