(ns multi-money.db.xtdb
  (:require [clojure.pprint :refer [pprint]]
            [xtdb.node :as xtn]
            [xtdb.client :as xtc]
            [xtdb.api :as xt]
            [multi-money.db :as db]))

(derive clojure.lang.PersistentArrayMap ::map)

(defn- put*
  "Give a list of model maps, or vector tuples with an action in the
  1st position and a model in the second, execute the actions and
  return the id values of the models"
  [_node docs]
  {:pre [(sequential? docs)]}

  (pprint {::put* docs}))

(defn- select*
  [_node criteria options]
  (pprint {::select* criteria
           ::options options}))

(defn- delete*
  [_node models]
  (pprint {::delete* models}))

(defn- reset*
  [_node]
  (comment "This is a no-op with in-memory implementation"))

(defmulti ^:private init-node type)

(defmethod init-node String
  [url]
  (xtc/start-client url))

(defmethod init-node ::map
  [config]
  (xtn/start-node config))

(defmethod db/reify-storage :xtdb
  [{:keys [config]}]
  (with-open [node (init-node config) ]
    (reify db/Storage
      (put    [_ models]           (put* node models))
      (select [_ criteria options] (select* node criteria options))
      (delete [_ models]           (delete* node models))
      (close  [_]                  (.close node))
      (reset  [_]                  (reset* node)))))
