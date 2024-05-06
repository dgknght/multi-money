(ns multi-money.models.entities
  (:refer-clojure :exclude [find])
  (:require [clojure.spec.alpha :as s]
            [multi-money.util :refer [->id]]
            [multi-money.db :as db]))

(s/def :entity/name string?)
(s/def ::entity (s/keys :req [:entity/name]))

(defn select
  ([criteria] (select criteria {}))
  ([criteria options]
   {:pre [(s/valid? ::db/options options)]}
   (map #(db/set-meta % :entity)
        (db/select (db/storage)
                    (db/model-type criteria :entity)
                    (update-in options [:order-by] (fnil identity [:name]))))))

(defn find
  [id]
  (first (select {:id (->id id)} {:limit 1})))

(defn- resolve-put-result
  [x]
  (if (map? x)
    (db/model-type x :entity)
    (find x)))

(defn put
  [entity]
  {:pre [entity (s/valid? ::entity entity)]}

  (let [records-or-ids (db/put (db/storage)
                                [entity])]
    (resolve-put-result (first records-or-ids)))) ; TODO: return all of the saved models instead of the first?

(defn delete
  [entity]
  {:pre [entity (map? entity)]}
  (db/delete (db/storage) [entity]))
