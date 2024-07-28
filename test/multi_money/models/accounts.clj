(ns multi-money.models.accounts
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
      (select-keys [:account/name :account/entity])
      (update-in [:account/entity] ->id)
      (exclude-self e)
      find-by
      nil?))
(v/reg-spec name-is-unique? {:message "%s is already in use"
                             :path [:account/name]})

(s/def :account/name string?)
(s/def :account/type #{:asset :liability :income :expense :equity})
(s/def :account/entity db/model-or-ref?)
(s/def :account/commodity db/model-or-ref?)
(s/def ::account (s/and (s/keys :req [:account/name
                                      :account/type
                                      :account/entity
                                      :account/commodity])
                       name-is-unique?))

(defn select
  [criteria & {:as options}]
  {:pre [(or (nil? options)
             (s/valid? ::db/options options))]}

  (map db/set-meta
       (db/select (db/storage)
                  (-> criteria
                      db/normalize-model-refs
                      (db/model-type :account))
                  (update-in options [:order-by] (fnil identity [:name])))))

(defn count
  ([] (count {}))
  ([criteria]
   (db/select (db/storage)
              (db/model-type criteria :account)
              {:count true})))

(defn find-by
  [criteria & {:as options}]
  (first (apply select criteria (mapcat identity (assoc options :limit 1)))))

(defn find
  [id]
  (find-by {:id (->id id)}))

(defn realize
  "Given a model that references an account, replace the account
  reference with the account model."
  [model k]
  (if (:account/name model)
    model
    (update-in model [k] find)))

(defn- resolve-put-result
  [x]
  (if (map? x)
    (db/model-type x :account)
    (find x)))

(defn put
  [account]

  (pprint {::put account})

  (v/with-ex-validation account ::account
    (let [records-or-ids (db/put (db/storage)
                                 [account])]
      ; TODO: return all of the saved models instead of the first?
      (resolve-put-result (first records-or-ids)))))

(defn delete
  [account]
  {:pre [account (map? account)]}
  (db/delete (db/storage) [account]))
