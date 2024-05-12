(ns multi-money.api.entities
  (:refer-clojure :exclude [update])
  (:require [dgknght.app-lib.api :refer [path]]
            [multi-money.api :as api]))

(defn select
  [& {:as opts}]
  (api/get (path :entities) opts))

(defn- create
  [entity opts]
  (api/post (path :entities) entity opts))

(defn- update
  [{:keys [id] :as entity} opts]
  (api/patch (path :entities id) entity opts))

(defn put
  [entity & {:as opts}]
  (if (:id entity)
    (update entity opts)
    (create entity opts)))

(defn delete
  [{:keys [id]} & {:as opts}]
  (api/delete (path :entities id) opts))
