(ns multi-money.db.sql.tasks
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts]]
            [java-time.api :as t]
            [config.core :refer [env]]
            [next.jdbc :as j]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [ragtime.strategy :refer [apply-new]]
            [multi-money.db.sql.partitioning :refer [create-partition-tables]]
            [multi-money.util :refer [mask-values]]
            [multi-money.db :as db]))

(defn- config []
  {:datastore (-> (db/config :sql)
                  (assoc :user (env :sql-ddl-user)
                         :password (env :sql-ddl-password))
                  jdbc/sql-database)
   :migrations (jdbc/load-resources "migrations")
   :strategy apply-new})

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
  (log/tracef "Creating database with config %s" (mask-values (env :db)
                                                              :user
                                                              :password))
  (let [{:keys [dbname] :as cfg} (db/config :sql)
        _ (assert (and (:dbtype cfg)
                       (:dbname cfg)
                       (:user cfg)
                       (:password cfg)
                       (:host cfg))
                  "The configuration is not valid.")
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

(def ^:private partition-opts
  [["-s" "--start START" "The start of the period for which partitions are to be created, inclusive."
    :parse-fn (partial t/local-date (t/formatter :iso-date))]
   ["-e" "--end START" "The end of the period for which partitions are to be created, exclusive."
    :parse-fn (partial t/local-date (t/formatter :iso-date))]
   ["-n" "--dry-run" "Only print the commands, do not execute them."]])

(defn partition
  [& args]
  (let [{{:as options :keys [start end]} :options} (parse-opts args partition-opts)]
    (pprint {::partition [start end]
             ::options options})
    (create-partition-tables start end (dissoc options :start :end))))
