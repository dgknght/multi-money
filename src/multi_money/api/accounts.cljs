(ns multi-money.api.accounts
  (:refer-clojure :exclude [update])
  (:require [cljs.pprint :refer [pprint]]
            [dgknght.app-lib.api :refer [path]]
            [multi-money.state :refer [current-entity]]
            [multi-money.api :as api]))

(defn- accounts-path []
  (if-let [entity-id (:id @current-entity)]
    (path :entities entity-id :accounts)
    (throw (js/Error. "No entity available"))))

(defn select
  [& {:as opts}]
  (api/get (accounts-path) opts))

(defn- create
  [account opts]
  (api/post (accounts-path) account opts))

(defn- update
  [{:keys [id] :as account} opts]
  (api/patch (path :accounts id) account opts))

(defn put
  [account & {:as opts}]
  (if (:id account)
    (update account opts)
    (create account opts)))

(defn delete
  [{:keys [id]} & {:as opts}]
  (api/delete (path :accounts id) opts))
