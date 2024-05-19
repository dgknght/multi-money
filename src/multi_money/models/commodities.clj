(ns multi-money.models.commodities
  (:refer-clojure :exclude [find])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id
                                      exclude-self
                                     non-nil?]]
            [multi-money.db :as db]))

(declare find-by)

(defn- symbol-is-unique?
  [e]
  (-> e
      (select-keys [:commodity/symbol :commodity/entity])
      (update-in [:commodity/entity] ->id)
      (exclude-self e)
      find-by
      nil?))
(v/reg-spec symbol-is-unique? {:message "%s is already in use"
                               :path [:commodity/symbol]})

(s/def :commodity/entity non-nil?)
(s/def :commodity/name string?)
(s/def :commodity/symbol string?)
(s/def :commodity/type #{:currency :stock :mutual-fund})
(s/def ::commodity (s/and (s/keys :req [:commodity/entity
                                        :commodity/name
                                        :commodity/symbol
                                        :commodity/type])
                          symbol-is-unique?))

(defn select
  [criteria & {:as options}]
  {:pre [(or (nil? options)
             (s/valid? ::db/options options))]}
  
  (map db/set-meta
       (db/select (db/storage)
                  (db/model-type criteria :commodity)
                  options)))

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
