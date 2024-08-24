(ns multi-money.db.datomic.accounts
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.criteria :as crt]
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
  (crt/apply-to criteria ->ids))
