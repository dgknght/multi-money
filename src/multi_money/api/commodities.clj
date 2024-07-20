(ns multi-money.api.commodities
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.authorization :as auth]
            [dgknght.app-lib.api :as api]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :as utl]
            [multi-money.db :as db]
            [multi-money.models.commodities :as cdts]
            [multi-money.authorization.commodities]))

(defn- extract-commodity
  [{:keys [body path-params]}]
  (-> body
      (utl/select-namespaced-keys
        [:commodity/symbol
         :commodity/name
         :commodity/type])
      (assoc :commodity/entity {:id (:entity-id path-params)})
      (update-in-if [:commodity/type] keyword)))

(defn- create
  [req]
  (-> req
      extract-commodity
      cdts/put
      api/creation-response))

(defn- extract-criteria
  [{:keys [path-params]}]
  {:commodity/entity (:entity-id path-params)})

(defn- index
  [{:as req :keys [authenticated]}]
  (-> req
      extract-criteria
      (db/model-type :commodity)
      (auth/+scope authenticated)
      cdts/select
      api/response))

(defn- find-and-authorize
  [{:keys [path-params authenticated]} action]
  (some-> path-params
          (select-keys [:id])
          (db/model-type :commodity)
          (auth/+scope authenticated)
          cdts/find-by
          (auth/authorize action authenticated)))

(defn- update
  [req]
  (if-let [commodity (find-and-authorize req ::auth/update)]
    (-> commodity
        (merge (dissoc (extract-commodity req) :commodity/entity))
        cdts/put
        api/response)
    api/not-found))

(defn- delete
  [req]
  (if-let [commodity (find-and-authorize req ::auth/destroy)]
    (do
      (cdts/delete commodity)
      api/no-content)
    api/not-found))

(def routes
  [["/entities/:entity-id/commodities"
    {:get {:handler index}
     :post {:handler create}} ]
   ["/commodities/:id"
    {:patch {:handler update}
     :delete {:handler delete}}]])
