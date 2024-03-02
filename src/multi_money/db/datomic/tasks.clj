(ns multi-money.db.datomic.tasks
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [datomic.client.api :as d]
            [multi-money.db :as db]))

(defn- schema []
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
  ([] (apply-schema :datomic))
  ([config-key]
   (let [{:as cfg
          :keys [db-name]} (dissoc (db/config config-key)
                                   ::db/provider)]
     (assert cfg (str "No datomic configuration found for " config-key))
     (apply-schema (d/client cfg)
                   db-name)))
  ([client db-name & {:keys [suppress-output?]}]
   (try
     (let [res (d/create-database client {:db-name db-name})]
       (when-not suppress-output?
         (pprint {::create-database res})))
     (catch AbstractMethodError _
       (println "The create-database function is not availabled. Skipping database creation.")))
   (let [res (d/transact (d/connect client {:db-name db-name})
                        {:tx-data (schema)
                         :db-name db-name})]
     (when-not suppress-output?
       (pprint {::transact-schema res})))))
