(ns multi-money.models.entities
  (:refer-clojure :exclude [find])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id]]
            [multi-money.db :as db]))

(declare find-by)

(defn- name-is-unique?
  [e]
  (-> e
      (select-keys [:id :entity/name :entity/owner])
      (update-in [:entity/owner] ->id)
      find-by
      nil?))
(v/reg-spec name-is-unique? {:message "%s is already in use"
                             :path [:entity/name]})

(s/def :entity/name string?)
(s/def ::entity (s/and (s/keys :req [:entity/name])
                       name-is-unique?))

(defn select
  ([criteria] (select criteria {}))
  ([criteria options]
   {:pre [(s/valid? ::db/options options)]}
   (map #(db/set-meta % :entity)
        (db/select (db/storage)
                    (db/model-type criteria :entity)
                    (update-in options [:order-by] (fnil identity [:name]))))))

(defn find-by
  [criteria]
  (first (select criteria {:limit 1})))

(defn find
  [id]
  (find-by {:id (->id id)}))

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
