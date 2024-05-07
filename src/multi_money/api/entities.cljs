(ns multi-money.api.entities)

(defn select
  [& {:keys [on-success callback]}]
  (on-success [])
  (callback))
