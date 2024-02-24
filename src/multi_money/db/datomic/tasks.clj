(ns multi-money.db.datomic.tasks
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [config.core :refer [env]]
            [datomic.client.api :as d]
            [multi-money.db :as db]))

(defn- schema []
  (mapcat (comp edn/read-string
                slurp
                io/resource
                #(format "datomic/schema/%s.edn" %))
          ["user"
           #_"account"
           #_"commodity"
           #_"entity"
           "model"
           #_"price"
           #_"transaction"]))

(defn apply-schema
  ([] (apply-schema :datomic))
  ([config-key]
   (let [{:as cfg
          :keys [db-name]} (dissoc (get-in env [:db
                                                :strategies
                                                config-key])
                                   ::db/provider)]
     (assert cfg (str "No datomic configuration found for " config-key))
     (apply-schema (d/client cfg)
                   db-name)))
  ([client db-name]
   (d/create-database client {:db-name db-name})
   (d/transact (d/connect client {:db-name db-name})
               {:tx-data (schema)
                :db-name db-name})))
