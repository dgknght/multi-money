(ns multi-money.datalog
  (:require [clojure.pprint :refer [pprint]]
            [stowaway.datalog :as dtl]))

(def ^:private default-opts
  {:relationships #{[:user :entity]
                    [:entity :commodity]}
   :query-prefix [:query]})

(defn apply-criteria
  [query criteria & {:as opts}]
  (dtl/apply-criteria query
                      criteria
                      (merge default-opts opts)))
