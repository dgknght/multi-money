(ns multi-money.db.datomic.accounts
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [update-in-criteria]]
            [multi-money.db.datomic :as d :refer [->id]]))

(defn- adjust-ids
  [m]
  (reduce #(update-in-criteria %1 [%2] ->id)
          m
          [:account/entity
           :account/commodity
           :account/parent]))

(defmethod d/before-save :account
  [account]
  (adjust-ids account))

(defmethod d/prepare-criteria :account
  [criteria]
  (adjust-ids criteria))
