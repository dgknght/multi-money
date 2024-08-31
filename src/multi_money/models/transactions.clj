(ns multi-money.models.transactions
  (:refer-clojure :exclude [find count])
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [java-time.api :as t]
            [dgknght.app-lib.validation :as v]
            [multi-money.db :as db]
            [multi-money.util :as utl]
            [multi-money.accounts :refer [polarize]]
            [multi-money.transactions :as trx]
            [multi-money.models.accounts :as acts]))

(derive clojure.lang.PersistentVector ::vector)
(derive clojure.lang.PersistentArrayMap ::map)
(derive clojure.lang.PersistentHashMap ::map)

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
(s/def :transaction/items (s/coll-of ::transaction-item :min-count 1 :kind vector?))
(s/def ::transaction (s/and (s/keys :req [:transaction/date
                                          :transaction/description
                                          :transaction/entity
                                          :transaction/items]
                                    :opt [:transaction/memo])
                            entities-match?))

(defmulti ^:private specifies-date-range? type)

(defmethod specifies-date-range? ::map
  [{:transaction/keys [date]}]
  (not (not date)))

(defmethod specifies-date-range? ::vector
  [[oper & cs]]
  (if (= oper :and)
    (some specifies-date-range? cs)
    (every? specifies-date-range? cs)))

(defn- date-range
  [trxs]
  (let [sorted-dates (->> trxs
                          (map :transaction/date)
                          (sort-by t/before?))]
    [(first sorted-dates) (last sorted-dates)]))

(defn- mass-append-items
  [trxs]
  (let [ids (mapv :id trxs)
        [start-date end-date] (date-range trxs)
        items (->> (db/select (db/storage)
                              {:transaction-item/transaction-id [:in ids]
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

(defn- normalize-account-refs
  [{:transaction/keys [account] :as criteria}]
  (if account
    [:and
     (dissoc criteria :transaction/account)
     [:or
      {:transaction-item/debit-account account}
      {:transaction-item/credit-account account}]]
    criteria))

(defn select
  [criteria & {:as options}]
  {:pre [(specifies-date-range? criteria)
         (s/valid? (s/nilable ::db/options) options)]}
  (post-select
    (map db/set-meta
         (db/select (db/storage)
                    (-> criteria
                        db/normalize-model-refs
                        normalize-account-refs
                        (db/model-type :transaction))
                    (update-in options [:order-by] (fnil identity [:name]))))))

(defn count
  ([] (count {}))
  ([criteria]
   {:pre [(specifies-date-range? criteria)]}
   (db/select (db/storage)
              (db/model-type criteria :transaction)
              {:count true})))

(defn find-by
  [criteria & {:as options}]
  (first (apply select criteria (mapcat identity (assoc options :limit 1)))))

(defn find
  ([m]
   {:pre [(:id m) (:transaction/date m)]}
   (-> m
       (select-keys [:id :transaction/date])
       find-by))
  ([id date]
   (find-by {:id id :transaction/date date})))

(defn- resolve-put-result
  [x date]
  (if (map? x)
    (db/model-type x :transaction)
    (find x date)))

(defn- resolve-account
  [account]
  (if (:account/name account)
    account
    (acts/find account)))

(def ^:private extract-debit
  (juxt (constantly :debit)
        (comp resolve-account
              :transaction-item/debit-account)
        :transaction-item/quantity))

(def ^:private extract-credit
  (juxt (constantly :credit)
        (comp resolve-account
              :transaction-item/credit-account)
        :transaction-item/quantity))

(defn- adj-act-balance
  [[action account quantity]]
  (update-in account [:account/balance] + (polarize quantity action account)))

(defn- update-1st-trx-date
  [model date k]
  (update-in model [k] #(->> [% date]
                               (filter identity)
                               (sort t/before?)
                               first)))

(defn- update-last-trx-date
  [model date k]
  (update-in model
             [k]
             #(->> [% date]
                   (filter identity)
                   (sort t/after?)
                   first)))

(defn- affected-accounts
  [date items]
  (->> items
       (mapcat (juxt extract-debit
                     extract-credit))
       (map (comp #(update-1st-trx-date % date :account/first-transaction-date)
                  #(update-last-trx-date % date :account/last-transaction-date)
                  adj-act-balance))))

(defn- update-entity
  [entity date]
  (-> entity
      (update-1st-trx-date date :entity/first-transaction-date)
      (update-last-trx-date date :entity/last-transaction-date)))

(defn- previous-item
  [account date]
  (->> (select {:transaction/account account
                :transaction/date [:<= date]}
               {:order-by [[:transaction/date :desc]]})
       (mapcat #(trx/per-account account %))
       (sort-by trx/index >)
       first))

(defn- gather-accounts
  [{:as m :keys [transaction]}]
  (assoc m :accounts (->> (:transaction/items transaction)
                          (mapcat (juxt :transaction-item/debit-account
                                        :transaction-item/credit-account))
                          (reduce (fn [res {:keys [id] :as a}]
                                    (assoc res id (if (:account/name a)
                                                    a
                                                    (acts/find a))))
                                  {}))))

(defn- propagate-items
  [account transaction]
  (let [prev-item (previous-item account (:transaction/date transaction))
        [prev-idx prev-bal] (if prev-item
                              [(trx/index prev-item)
                               (trx/balance prev-item)]
                              [0 0M])]
    (->> (trx/per-account account [transaction])
         (map (fn [item]
                (-> item
                    (trx/index (inc prev-idx))
                    (trx/balance (+ prev-bal (trx/balance item)))))))))

(defn- propagate-and-append-items
  [{:as m :keys [accounts transaction]}]
  (reduce (fn [res account]
            (assoc-in res
                      [:affected-items (:id account)]
                      (propagate-items account transaction)))
          m
          accounts))

(defn propagate-transaction
  [transaction]
  {:pre [(vector? (:transaction/items transaction))
         (t/local-date? (:transaction/date transaction))]}
  [transaction]
  #_[(-> {:transaction transaction}
       gather-accounts
       propagate-and-append-items
       update-entity
       extract-puts)])

(defn put
  [{:as transaction :transaction/keys [date]}]
  (v/with-ex-validation transaction ::transaction
    (let [records-or-ids (db/put (db/storage)
                                 (propagate-transaction transaction))]
      ; TODO: return all of the saved models instead of the first?
      (resolve-put-result (first records-or-ids) date))))

(defn delete
  [transaction]
  {:pre [transaction (map? transaction)]}
  (db/delete (db/storage) [transaction]))
