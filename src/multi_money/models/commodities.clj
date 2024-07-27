(ns multi-money.models.commodities
  (:refer-clojure :exclude [find count])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id
                                      ->model-ref
                                      exclude-self]]
            [multi-money.db :as db]))

(declare find-by)

(defn- symbol-is-unique?
  [e]
  (-> e
      (select-keys [:commodity/symbol :commodity/entity])
      (exclude-self e)
      find-by
      nil?))
(v/reg-spec symbol-is-unique? {:message "%s is already in use"
                               :path [:commodity/symbol]})

(s/def :commodity/entity db/model-or-ref?)
(s/def :commodity/name string?)
(s/def :commodity/symbol string?)
(s/def :commodity/type #{:currency :stock :mutual-fund})
(s/def ::commodity (s/and (s/keys :req [:commodity/entity
                                        :commodity/name
                                        :commodity/symbol
                                        :commodity/type])
                          symbol-is-unique?))

(defn- select*
  [criteria options]
  (db/select (db/storage)
             (-> criteria
                 db/normalize-model-refs
                 (db/model-type :commodity))
             options))

(defn select
  [criteria & {:as options}]
  {:pre [(or (nil? options)
             (s/valid? ::db/options options))]}
  (map (comp db/set-meta
             #(update-in % [:commodity/entity] ->model-ref))
       (select* criteria options)))

(defn count
  ([] (count {}))
  ([criteria]
   (select* criteria {:count true})))

(defn find-by
  [criteria & {:as options}]
  (first (apply select criteria (mapcat identity (assoc options :limit 1)))))

(defn find
  [id]
  (find-by {:id (->id id)}))

(defn- before-save
  [commodity]
  (db/model-type commodity :commodity))

(defn put
  [commodity]
  (v/with-ex-validation commodity ::commodity
    (let [ids (db/put (db/storage) [(before-save commodity)])]
      (find (first ids)))))

(defn delete
  [commodity]
  {:pre [(:id commodity)]}
  (db/delete (db/storage) [commodity]))
