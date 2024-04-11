(ns multi-money.db.sql.tasks
  (:require [clojure.pprint :refer [pprint]]
            [config.core :refer [env]]
            [next.jdbc :as j]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [multi-money.db :as db]))

(defn- config []
  {:datastore (-> (db/config :sql)
                  (assoc :user (env :sql-ddl-user)
                         :password (env :sql-ddl-password))
                  jdbc/sql-database)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (println "migrating the database...")
  (let [cfg (config)]
    (repl/migrate cfg))
  (println "done."))

(defn rollback []
  (println "rolling back the database...")
  (repl/rollback (config))
  (println "done."))

(defn remigrate []
  (println "remigrating the database...")
  (let [cfg (config)]
    (repl/rollback cfg)
    (repl/migrate cfg))
  (println "done."))

(def ^:private role-cmd
  "create role %s with LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '%s'")

(def ^:private role-exists-cmd
  "select 1 from pg_roles where rolname = '%s'")

(defn- init-cmds
  [dbname]
  [{:label "ddl user"
    :exists? (format role-exists-cmd (env :sql-ddl-user))
    :create (format role-cmd (env :sql-ddl-user) (env :sql-ddl-password))}
   {:label "app user"
    :exists? (format role-exists-cmd (env :sql-app-user))
    :create (format role-cmd (env :sql-app-user) (env :sql-app-password))}
   {:label "database"
    :exists? (format "SELECT 1 FROM pg_database WHERE datname = '%s'"
                     dbname)
    :create (format "create database %s with owner = %s"
                    dbname
                    (env :sql-ddl-user))}])

(defn create
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
               j/get-datasource)]
    (doseq [{:keys [exists? create label]} (init-cmds dbname)]
      (if (empty? (j/execute! ds [exists?]))
        (do
          (println (format "Creating %s..." label))
          (j/execute! ds [create])
          (println "done."))
        (println (format "%s already exists." label))))))
