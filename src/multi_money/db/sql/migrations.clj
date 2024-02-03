(ns multi-money.db.sql.migrations
  (:require [clojure.pprint :refer [pprint]]
            [next.jdbc :as j]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [multi-money.db :as db]))

(defn- config []
  {:datastore (jdbc/sql-database (db/config :sql))
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (let [cfg (config)]
    (repl/migrate cfg)))

(defn rollback []
  (repl/rollback (config)))

(defn remigrate []
  (let [cfg (config)]
    (repl/rollback cfg)
    (repl/migrate cfg)))

(defn create-db []
  (let [{:keys [dbname] :as cfg} (:datastore (config))
        ds (-> cfg
               (assoc :dbname "postgres")
               (assoc :classname "org.postgresql.Driver")
               j/get-datasource)]
    (j/execute! ds ["create database %" dbname])))
