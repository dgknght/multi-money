(ns multi-money.db.sql.migrations
  (:require [clojure.pprint :refer [pprint]]
            [config.core :refer [env]]
            [next.jdbc :as j]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [multi-money.db :as db])
  (:import org.postgresql.util.PSQLException))

(defn- config []
  {:datastore (-> (db/config :sql)
                  (assoc :user (env :sql-ddl-user)
                         :password (env :sql-ddl-password))
                  jdbc/sql-database)
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

(def ^:private role-cmd
  "create role %s with LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '%s'")

(defn init
  "Creates the database and users specified in the configuration.

  We assume that by default there is a database with the same
  name as the user. We connect using that database, and then create
  the one that the app is configured to us."
  []
  (let [{:keys [dbname] :as cfg} (db/config :sql)
        ds (-> cfg
               (assoc :dbname   (env :sql-adm-user)
                      :user     (env :sql-adm-user)
                      :password (env :sql-adm-password))
               j/get-datasource)
        stmts (map (fn [[fmt & args]]
                     (apply format fmt args))
                   [[role-cmd
                     (env :sql-ddl-user)
                     (env :sql-ddl-password)]
                    [role-cmd
                     (env :sql-app-user)
                     (env :sql-app-password)]
                    ["create database %s with owner = %s"
                     dbname
                     (env :sql-ddl-user)]])]
    (doseq [s stmts]
      (try
        (j/execute! ds [s])
        (catch PSQLException e
          (when-not (re-find #"already exists" (.getMessage e))
            (throw e)))))))
