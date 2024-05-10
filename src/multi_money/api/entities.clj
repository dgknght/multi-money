(ns multi-money.api.entities
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.authorization :refer [+scope]]
            [dgknght.app-lib.api :as api]
            [multi-money.db :as db]
            [multi-money.util :refer [qualify-keys]]
            [multi-money.models.entities :as ents]
            [multi-money.authorization.entities]))

(defn- extract-entity
  [{:keys [body]}]
  (-> body
      (select-keys [:name])
      (qualify-keys :entity)))

(defn- create
  [{:as req :keys [authenticated]}]
  (-> req
      extract-entity
      (assoc :entity/owner authenticated)
      ents/put
      api/creation-response))

(defn- extract-criteria
  [_req]
  {})

(defn- index
  [{:as req :keys [authenticated]}]
  (-> req
      extract-criteria
      (db/model-type :entity)
      (+scope authenticated)
      ents/select
      api/response))

(defn- find-and-authorize
  [{:keys [path-params]}]
  ; TODO: Add authorization
  (ents/find (:id path-params)))

(defn- update
  [req]
  (if-let [entity (find-and-authorize req)]
    (-> entity
        (merge (extract-entity req))
        ents/put
        api/response)
    api/not-found))

(defn- delete
  [req]
  (if-let [entity (find-and-authorize req)]
    (do (ents/delete entity)
        api/no-content)
    api/not-found))


(def routes
  ["/entities"
   ["" {:get {:handler index}
        :post {:handler create}}]
   ["/:id" {:patch {:handler update}
            :delete {:handler delete}}]])
