(ns multi-money.api.accounts
  (:refer-clojure :exclude [update])
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.authorization :as auth]
            [dgknght.app-lib.api :as api]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :as utl]
            [multi-money.db :as db]
            [multi-money.models.entities :as ents]
            [multi-money.models.accounts :as acts]
            [multi-money.authorization.accounts]))

(defn- extract-account
  [{:keys [body]}]
  (-> body
      (utl/select-namespaced-keys [:account/name
                                   :account/type
                                   :account/commodity
                                   :account/parent])
      (update-in-if [:account/type] keyword)))

(defn- ensure-commodity
  [{:account/keys [commodity] :as account}]
  (if commodity
    account
    (let [{:entity/keys [default-commodity]}
          (:account/entity (ents/realize account :account/entity))]
      (assoc account :account/commodity default-commodity))))

(defn- create
  [{:as req :keys [path-params authenticated]}]
  (-> req
      extract-account
      (assoc :account/entity {:id (:entity-id path-params)})
      ensure-commodity
      (auth/authorize ::auth/create authenticated)
      acts/put
      api/creation-response))

(defn- extract-criteria
  [{:keys [path-params]}]
  {:account/entity {:id (:entity-id path-params)}})

(defn- index
  [{:as req :keys [authenticated]}]
  (-> req
      extract-criteria
      (db/model-type :account)
      (auth/+scope authenticated)
      acts/select
      api/response))

(defn- find-and-authorize
  [{:keys [path-params authenticated]} action]
  (some-> (:id path-params)
          acts/find
          (auth/authorize action authenticated)))

(defn- update
  [req]
  (if-let [account (find-and-authorize req ::auth/update)]
    (-> account
        (merge (extract-account req))
        acts/put
        api/response)
    api/not-found))

(defn- delete
  [req]
  (if-let [account (find-and-authorize req ::auth/destroy)]
    (do (acts/delete account)
        api/no-content)
    api/not-found))


(def routes
  [["/entities/:entity-id/accounts" {:get {:handler index}
                                     :post {:handler create}}]
   ["/accounts/:id" {:patch {:handler update}
                     :delete {:handler delete}}]])
