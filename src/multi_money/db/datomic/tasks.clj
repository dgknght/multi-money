(ns multi-money.db.datomic.tasks
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [multi-money.db :as db]))

(defn schema []
  (mapcat (comp edn/read-string
                slurp
                io/resource
                #(format "datomic/schema/%s.edn" %))
          ["model"
           "user"
           #_"entity"
           #_"commodity"
           #_"price"
           #_"account"
           #_"transaction"]))

(defn apply-schema
  ([] (apply-schema :datomic-peer))
  ([config-key]
   (let [{:as cfg
          :keys [db-name]} (dissoc (db/config config-key)
                                   ::db/provider)]
     (assert cfg (str "No datomic configuration found for " config-key))
     (apply-schema cfg
                   db-name)))
  ([{:keys [uri]} {:keys [suppress-output?]}]
   (d/create-database uri)
   (let [res (d/transact (d/connect uri)
                         (schema))]
     (when-not suppress-output?
       (pprint {::transact-schema @res})))))
