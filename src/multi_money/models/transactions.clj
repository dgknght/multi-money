(ns multi-money.models.transactions
  (:refer-clojure :exclude [find count])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [dgknght.app-lib.validation :as v]
            [multi-money.util :refer [->id]]
            [multi-money.db :as db]
            [multi-money.models.accounts :as acts]))

(declare find-by)

(defn- entities-match?
  [{:transaction/keys [items entity]}]
  (let [ids (->> items
                 (mapcat (juxt :transaction-item/debit-account
                               :transaction-item/credit-account))
                 (map (comp
                        #(get-in % [:account/entity :id])
                        #(if (db/model-ref? %)
                           (acts/find %)
                           %)))
                 (into #{(:id entity)}))]
    (= 1 (clojure.core/count ids))))
(v/reg-spec entities-match? {:message "All items must have accounts that belong to the same entity as the transaction"
                             :path [:transaction/items]})

(s/def :transaction-item/quantity decimal?)
(s/def :transaction-item/debit-account db/model-or-ref?)
(s/def :transaction-item/credit-account db/model-or-ref?)
(s/def ::transaction-item (s/keys :req [:transaction-item/quantity
                                        :transaction-item/debit-account
                                        :transaction-item/credit-account]))
(s/def :transaction/date t/local-date?)
(s/def :transaction/description string?)
(s/def :transaction/memo (s/nilable string?))
(s/def :transaction/entity db/model-or-ref?)
(s/def :transaction/items (s/coll-of ::transaction-item :min-count 1))
(s/def ::transaction (s/and (s/keys :req [:transaction/date
                                          :transaction/description
                                          :transaction/entity
                                          :transaction/items]
                                    :opt [:transaction/memo])
                            entities-match?))

(defn- date-range
  [trxs]
  (let [sorted-dates (->> trxs
                          (map :transaction/date)
                          (sort-by t/before?))]
    [(first sorted-dates) (last sorted-dates)]))

(defn- mass-append-items
  [trxs]
  (let [ids (map :id trxs)
        [start-date end-date] (date-range trxs)
        items (->> (db/select (db/storage)
                              {:transaction-item/transaction-id ids
                               :transaction-item/date [:between start-date end-date]}
                              {})
                   (group-by #(get-in % [:transaction-item/transaction :id])))]
    (map #(assoc % :transaction/items (items (:id %))) trxs)))

(defn- lacks-items?
  [[trx :as trxs]]
  (and (seq trxs)
       (nil? (:transaction/items trx))))

(defn- post-select
  [trxs]
  (if (lacks-items? trxs)
    (->> trxs
         (partition-all 10)
         (mapcat mass-append-items))
    trxs))

(defn select
  [criteria & {:as options}]
  {:pre [(s/valid? (s/nilable ::db/options) options)]}

  (post-select
    (map db/set-meta
         (db/select (db/storage)
                    (-> criteria
                        db/normalize-model-refs
                        (db/model-type :transaction))
                    (update-in options [:order-by] (fnil identity [:name]))))))

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
