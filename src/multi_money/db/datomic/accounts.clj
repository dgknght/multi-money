(ns multi-money.db.datomic.accounts
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.util :refer [apply-to-criteria]]
            [multi-money.db.datomic :as d]))

(declare ->ids)
(d/def->ids ->ids
  :account/entity
  :account/commodity
  :account/parent)

(defmethod d/before-save :account
  [account]
  (->ids account))

(defmethod d/prepare-criteria :account
  [criteria]
  (apply-to-criteria criteria ->ids))
