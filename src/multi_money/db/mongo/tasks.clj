(ns multi-money.db.mongo.tasks
  (:require [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [somnium.congomongo :as m]
            [multi-money.db :as db]
            [multi-money.db.mongo :as mongo])
  (:import com.mongodb.MongoCommandException))

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
    (if (empty? (m/fetch :system.users :where {:user (env :mongo-app-user)}))
      (let [roles (doto (com.mongodb.BasicDBObject.)
                    (.append "role" "readWrite")
                    (.append "db" (env :mongo-db-name)))
            cmd (doto (com.mongodb.BasicDBObject.)
                  (.append "createUser" (env :mongo-app-user))
                  (.append "pwd" (env :mongo-app-password))
                  (.append "roles" (to-array [roles])))]
        (m/with-db (env :mongo-db-name)
          (pprint {::init (try (m/command! cmd)
                               (catch MongoCommandException e
                                 {:code (.getErrorCode e)
                                  :message (.getErrorMessage e)}))})))
      (println (format "%s already exists." (env :mongo-app-user))))))

(defn index
  "Apply any new indexes to the database"
  []
  (let [cfg (db/config :mongo)]
    (assert (and (:host cfg)
                 (:database cfg)
                 (:username cfg)
                 (:password cfg))
            "The configuration is missing or incomplete.")
    (m/with-mongo (mongo/connect cfg)
      (pprint {::users-email      (m/add-index! :users
                                                [:email]
                                                :unique true)})
      (pprint {::users-identities (m/add-index! :users
                                                [:identities.id :identities.provider]
                                                :unique true)})
      (pprint {::entities-owner (m/add-index! :entities
                                              [:owner_id :name]
                                              :unique true)}))))
