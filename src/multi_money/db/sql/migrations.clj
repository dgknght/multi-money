(ns multi-money.db.sql.migrations
  (:require [clojure.pprint :refer [pprint]]
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
