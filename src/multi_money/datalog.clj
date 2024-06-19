(ns multi-money.datalog
  (:require [clojure.spec.alpha :as s]
            [clojure.pprint :refer [pprint]]
            [stowaway.datalog :as dtl]))

(def ^:private default-opts
  {:relationships #{[:user :entity]
                    [:entity :commodity]}
   :query-prefix [:query]})

(defn apply-criteria
  [query criteria & {:as opts}]
  {:pre [(or (nil? opts)
             (s/valid? ::options opts))]}
  (dtl/apply-criteria query
                      criteria
                      (merge default-opts opts)))
