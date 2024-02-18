(ns multi-money.db.mongo.tasks
  (:require [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [somnium.congomongo :as m]
            [multi-money.db :as db]
            [multi-money.db.mongo :as mongo]))

(defn- admin-conn []
  (-> (db/config :mongo)
      (assoc :database "admin"
             :username (env :mongo-adm-user)
             :password (env :mongo-adm-password))
      mongo/connect))

(defn init
  "Initializes the users that will access the database"
  []
  (m/with-mongo (admin-conn)
    (let [roles (doto (com.mongodb.BasicDBObject.)
                  (.append "role" "readWrite")
                  (.append "db" (env :mongo-database)))
          cmd (doto (com.mongodb.BasicDBObject.)
                (.append "createUser" (env :mongo-app-user))
                (.append "pwd" (env :mongo-app-password))
                (.append "roles" (to-array [roles])))]
      (m/with-db (env :mongo-database)
        (pprint {::init (m/command! cmd)})))))

(defn index
  "Apply any new indexes to the database"
  []
  (m/with-mongo (admin-conn)
    (pprint {::users-email      (m/add-index! :users
                                              [:email]
                                              :unique true)})
    (pprint {::users-identities (m/add-index! :users
                                              [:identities.id :identities.provider]
                                              :unique true)})))
