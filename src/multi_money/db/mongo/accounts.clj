(ns multi-money.db.mongo.accounts
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [multi-money.db.mongo :as m]))

(defn- generalize-model-refs
  [account]
  (-> account
      (rename-keys {:account/entity-id :account/entity
                    :account/commodity-id :account/commodity})
      (update-in [:account/entity] #(hash-map :id %))
      (update-in [:account/commodity] #(hash-map :id %))))

(defmethod m/after-read :account
  [account]
  (-> account
      (update-in [:account/type] keyword)
      generalize-model-refs))

(defn- mongoify-model-refs
  [m]
  (m/mongoify-model-refs m {:account/entity :account/entity-id
                            :account/commodity :account/commodity-id}))

(defmethod m/prepare-criteria :account
  [criteria]
  (mongoify-model-refs criteria))

(defmethod m/before-save :account
  [account]
  (mongoify-model-refs account))
