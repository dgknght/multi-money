(ns multi-money.orchestration)

(defn- affected-accounts
  [items]
  (->> items
       (map (comp #(acts/resolve % :account/debit-account)
                  #(acts/resolve % :account/credit-account)))))

(defn propagate-transaction
  [{:as trx :transaction/keys [items]}]
  (conj trx (affected-accounts items)))
