(ns multi-money.models.transactions
  (:refer-clojure :exclude [find count])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id
                                      ->model-ref
                                      exclude-self]]
            [multi-money.db :as db]))

(declare find-by)

#_(defn- account-entities-match?
  [{:transaction/keys [:items]}]
  (if (seq items)
    (->> items
         (map #(get-in % [:transaction-item/account :account/entity]))
         (apply =))
    true))
#_(v/reg-spec name-is-unique? {:message "%s is already in use"
                             :path [:transaction/name]})

(s/def :transaction/date t/local-date?)
(s/def :transaction/description string?)
(s/def :transaction/memo string?)
(s/def :transaction/entity db/model-or-ref?)
(s/def :transaction/items (s/coll-of ::transaction-item :min-count 1))
(s/def ::transaction (s/keys :req [:transaction/date
                                   :transaction/description
                                   :transaction/entity
                                   :transaction/items]
                             :opt [:transaction/memo]))

(defn select
  [criteria & {:as options}]
  {:pre [(s/valid? (s/nilable ::db/options) options)]}

  (map db/set-meta
       (db/select (db/storage)
                  (-> criteria
                      db/normalize-model-refs
                      (db/model-type :transaction))
                  (update-in options [:order-by] (fnil identity [:name])))))

(defn count
  ([] (count {}))
  ([criteria]
   (db/select (db/storage)
              (db/model-type criteria :transaction)
              {:count true})))

(defn find-by
  [criteria & {:as options}]
  (first (apply select criteria (mapcat identity (assoc options :limit 1)))))

(defn find
  [id]
  (find-by {:id (->id id)}))

(defn- resolve-put-result
  [x]
  (if (map? x)
    (db/model-type x :transaction)
    (find x)))

(defn put
  [transaction]
  (v/with-ex-validation transaction ::transaction
    (let [records-or-ids (db/put (db/storage)
                                 [transaction])]
      ; TODO: return all of the saved models instead of the first?
      (resolve-put-result (first records-or-ids)))))

(defn delete
  [transaction]
  {:pre [transaction (map? transaction)]}
  (db/delete (db/storage) [transaction]))
