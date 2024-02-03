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

(defn create-db
  "Creates the database specified in the configuration.

  We assume that by default there is a database with the same
  name as the user. We connect using that database, and then create
  the one that the app is configured to us."
  []
  (let [{:keys [dbname user] :as cfg} (db/config :sql)
        ds (-> cfg
               (assoc :dbname user)
               j/get-datasource)]
    (j/execute! ds [(format "create database %s" dbname)])))
