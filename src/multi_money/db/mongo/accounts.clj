(ns multi-money.db.mongo.accounts
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.criteria :as crt]
            [multi-money.db.mongo :as m]))

(declare <-mongo-refs)
(m/def<-mongo-refs <-mongo-refs
  :account/entity
  :account/commodity
  :account/parent)

(defmethod m/after-read :account
  [account]
  (-> account
      (update-in [:account/type] keyword)
      <-mongo-refs))

(declare ->mongo-refs)
(m/def->mongo-refs ->mongo-refs
  :account/entity
  :account/commodity
  :account/parent)

(defmethod m/prepare-criteria :account
  [criteria]
  (crt/apply-to criteria ->mongo-refs))

(defmethod m/before-save :account
  [account]
  (->mongo-refs account))
