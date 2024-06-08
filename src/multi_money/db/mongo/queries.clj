(ns multi-money.db.mongo.queries
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [stowaway.mongo.queries :as qrys]
            [stowaway.mongo.pipelines :as pips]
            [somnium.congomongo.coerce :refer [coerce-ordered-fields]]
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

(defn criteria->query
  [criteria & [options]]
  (-> criteria
      (qrys/criteria->query (assoc options :coerce-id coerce-id))
      (update-in-if [:sort] coerce-ordered-fields)))

(def ^:private relationships
  #{[:users :entities]
    [:entities :commodities]})

(defn criteria->pipeline
  [criteria options]
  (pips/criteria->pipeline criteria
                          (assoc options
                                 :coerce-id coerce-id
                                 :relationships relationships)))
