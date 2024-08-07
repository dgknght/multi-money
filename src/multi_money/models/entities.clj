(ns multi-money.models.entities
  (:refer-clojure :exclude [find count])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id
                                      exclude-self]]
            [multi-money.db :as db]))

(declare find-by)

(defn- name-is-unique?
  [e]
  (-> e
      (select-keys [:entity/name :entity/owner])
      (update-in [:entity/owner] ->id)
      (exclude-self e)
      find-by
      nil?))
(v/reg-spec name-is-unique? {:message "%s is already in use"
                             :path [:entity/name]})

(s/def :entity/name string?)
(s/def ::entity (s/and (s/keys :req [:entity/name])
                       name-is-unique?))

(defn select
  [criteria & {:as options}]
  {:pre [(s/valid? (s/nilable ::db/options) options)]}

  (map db/set-meta
       (db/select (db/storage)
                  (-> criteria
                      db/normalize-model-refs
                      (db/model-type :entity))
                  (update-in options [:order-by] (fnil identity [:name])))))

(defn count
  ([] (count {}))
  ([criteria]
   (db/select (db/storage)
              (db/model-type criteria :entity)
              {:count true})))

(defn find-by
  [criteria & {:as options}]
  (first (apply select criteria (mapcat identity (assoc options :limit 1)))))

(defn find
  [id]
  (find-by {:id (->id id)}))

(defn realize
  "Given a model that references an entity, replace the entity
  reference with the entity model."
  [model k]
  (if (get-in model [k :entity/name])
    model
    (update-in model [k] find)))

(defn- resolve-put-result
  [x]
  (if (map? x)
    (db/model-type x :entity)
    (find x)))

(defn put
  [entity]
  (v/with-ex-validation entity ::entity
    (let [records-or-ids (db/put (db/storage)
                                 [entity])]
      ; TODO: return all of the saved models instead of the first?
      (resolve-put-result (first records-or-ids)))))

(defn delete
  [entity]
  {:pre [entity (map? entity)]}
  (db/delete (db/storage) [entity]))
