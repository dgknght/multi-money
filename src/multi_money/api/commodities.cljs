(ns multi-money.api.commodities
  (:refer-clojure :exclude [update])
  (:require [cljs.pprint :refer [pprint]]
            [dgknght.app-lib.api :refer [path]]
            [multi-money.state :refer [current-entity]]
            [multi-money.api :as api]))

(defn select
  [& {:as opts}]
  (if-let [entity-id (:id @current-entity)]
    (api/get (path :entities entity-id :commodities) opts)
    (throw (js/Error. "No entity selected"))))

(defn- create
  [commodity opts]
  (if-let [entity-id (:id @current-entity)]
    (api/post (path :entities entity-id :commodities)
              commodity
              opts)
    (throw (js/Error. "No entity selected"))))

(defn- update
  [{:keys [id] :as commodity} opts]
  (api/patch (path :commodities id) commodity opts))

(defn put
  [commodity & {:as opts}]
  (if (:id commodity)
    (update commodity opts)
    (create commodity opts)))

(defn delete
  [{:keys [id]} & {:as opts}]
  (api/delete (path :commodities id) opts))
