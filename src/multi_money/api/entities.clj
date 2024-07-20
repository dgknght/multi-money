(ns multi-money.api.entities
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.authorization :as auth]
            [dgknght.app-lib.api :as api]
            [multi-money.util :as utl]
            [multi-money.db :as db]
            [multi-money.models.entities :as ents]
            [multi-money.authorization.entities]))

(defn- extract-entity
  [{:keys [body]}]
  (utl/select-namespaced-keys body [:entity/name]))

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
      (auth/+scope authenticated)
      ents/select
      api/response))

(defn- find-and-authorize
  [{:keys [path-params authenticated]} action]
  (some-> (:id path-params)
          ents/find
          (auth/authorize action authenticated)))

(defn- update
  [req]
  (if-let [entity (find-and-authorize req ::auth/update)]
    (-> entity
        (merge (extract-entity req))
        ents/put
        api/response)
    api/not-found))

(defn- delete
  [req]
  (if-let [entity (find-and-authorize req ::auth/destroy)]
    (do (ents/delete entity)
        api/no-content)
    api/not-found))


(def routes
  ["/entities"
   ["" {:get {:handler index}
        :post {:handler create}}]
   ["/:id" {:patch {:handler update}
            :delete {:handler delete}}]])
