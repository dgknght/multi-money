(ns multi-money.orchestration
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.models.accounts :as acts]
            [multi-money.accounts :refer [polarize]]))


(defn- update-account
  [item action]
  (let [k (keyword "transaction-item"
                   (str (name action) "-account"))]
    (update-in item [k] (fn [act]
                          (update-in act [:quantity] + polarize action act)))))

(defn- affected-accounts
  [items]
  (->> items
       (map #(update-account % :debit))))

(defn propagate-transaction
  [{:as trx :transaction/keys [items]}]
  (cons trx (affected-accounts items)))
