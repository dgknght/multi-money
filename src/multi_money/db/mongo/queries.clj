(ns multi-money.db.mongo.queries
  (:require [clojure.set :refer [rename-keys]]
            [somnium.congomongo.coerce :refer [coerce-ordered-fields]]
            [camel-snake-kebab.core :refer [->snake_case]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [unqualify-keys]]
            [multi-money.db.mongo.types :refer [coerce-id]]))

; TODO: Move this into the mongo.accounts ns
#_(defn apply-account-id
  [{:keys [where] :as query} {:keys [account-id]}]
  (if-let [id (safe-coerce-id account-id)]
    (let [c {:$or
             [{:debit-account-id id}
              {:credit-account-id id}]}]
      (assoc query :where (if where
                            {:$and [where c]}
                            c)))
    query))

(def ^:private oper-map
  {:> :$gt
   :>= :$gte
   :< :$lt
   :<= :$lte})

(defmulti ^:private adjust-complex-criterion
  (fn [[_k v]]
    (when (vector? v)
      (let [[oper] v]
        (or (#{:and :or} oper)
            (when (oper-map oper) :comparison)
            (first v))))))

(defn- ->mongodb-op
  [op]
  (get-in oper-map
          [op]
          (keyword (str "$" (name op)))))

(defmethod adjust-complex-criterion :default [c] c)

(defmethod adjust-complex-criterion :comparison
  [[f [op v]]]
  ; e.g. [:transaction-date [:< #inst "2020-01-01"]]
  ; ->   [:transaction-date {:$lt #inst "2020-01-01"}]
  {f {(->mongodb-op op) v}})

(defmethod adjust-complex-criterion :and
  [[f [_ & cs]]]
  {f (->> cs
          (map #(update-in % [0] ->mongodb-op))
          (into {}))})

(defmethod adjust-complex-criterion :or
  [[f [_ & cs]]]
  {f {:$or (mapv (fn [[op v]]
                   {(->mongodb-op op) v})
                 cs)}})

(defmulti ^:private ->mongodb-sort
  (fn [x]
    (when (vector? x)
      :explicit)))

(defmethod ->mongodb-sort :default
  [x]
  [x 1])

(defmethod ->mongodb-sort :explicit
  [sort]
  (update-in sort [1] #(if (= :asc %) 1 -1)))
(defn- adjust-complex-criteria
  [criteria]
  (->> criteria
       (map adjust-complex-criterion)
       (into {})))

(defn apply-criteria
  [query criteria]
  (if (seq criteria)
    (assoc query :where (-> criteria
                            unqualify-keys
                            (update-in-if [:id] coerce-id)
                            (rename-keys {:id :_id})
                            adjust-complex-criteria))
    query))

(defn- apply-options
  [query {:keys [limit order-by]}]
  (cond-> query
    limit (assoc :limit limit)
    order-by (assoc :sort (coerce-ordered-fields (map ->mongodb-sort order-by)))))

(defn criteria->query
  ([criteria] (criteria->query criteria {}))
  ([criteria options]
   (-> {}
       (apply-criteria (dissoc criteria :account-id)) ; TODO: remove dissoc once we've moved account-id logic into mongo.accounts ns
       #_(apply-account-id criteria) ; TODO: Remove this once we've move account id logic into mongo.accounts ns
       (apply-options options))))
