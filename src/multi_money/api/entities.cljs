(ns multi-money.api.entities
  (:refer-clojure :exclude [select update])
  (:require [dgknght.app-lib.api-3 :as api]))

(defn select
  [& opts]
  (api/get (api/path :entities) opts))

(defn create
  [entity opts]
  (api/post (api/path :entities) entity opts))

(defn update
  [{:keys [id ] :as entity} opts]
  (api/patch (api/path :entities id) entity opts))

(defn put
  [entity & opts]
  (if (:id entity)
    (update entity opts)
    (create entity opts)))
