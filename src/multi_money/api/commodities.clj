(ns multi-money.api.commodities
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.authorization :as auth]
            [dgknght.app-lib.api :as api]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.db :as db]
            [multi-money.models.commodities :as cdts]
            [multi-money.authorization.commodities]))

(defn- extract-commodity
  [{:keys [body]}]
  (-> body
      (select-keys [:commodity/symbol
                    :commodity/name
                    :commodity/type
                    :commodity/entity])
      (update-in-if [:commodity/type] keyword)))

(defn- create
  [req]
  (-> req
      extract-commodity
      cdts/put
      api/creation-response))

(defn- extract-criteria
  [_req]
  {})

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
  (some-> {:id (:id path-params)}
          (db/model-type :commodity)
          (auth/+scope authenticated)
          cdts/find-by
          (auth/authorize action authenticated)))

(defn- update
  [req]
  (if-let [commodity (find-and-authorize req ::auth/update)]
    (-> commodity
        (merge (extract-commodity req))
        cdts/put
        api/response)
    api/not-found))

(defn- delete
  [req]
  (if-let [commodity (find-and-authorize req ::auth/destroy)]
    (do (cdts/delete commodity)
        api/no-content)
    api/not-found))

(def routes
  ["/commodities"
   ["" {:get {:handler index}
        :post {:handler create}}]
   ["/:id" {:patch {:handler update}
            :delete {:handler delete}}]])
