(ns multi-money.db.mongo
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [config.core :refer [env]]
            #_[java-time.api :as t]
            #_[cheshire.generate :refer [add-encoder]]
            [somnium.congomongo :as m]
            [somnium.congomongo.coerce :refer [#_ConvertibleFromMongo
                                               #_ConvertibleToMongo]]
            [dgknght.app-lib.inflection :refer [plural]]
            [multi-money.util :refer [qualify-keys
                                      unqualify-keys]]
            [multi-money.db :as db]
            [multi-money.db.mongo.queries :refer [criteria->query]]
            [multi-money.db.mongo.types :refer [coerce-id]]))

(derive clojure.lang.PersistentHashMap ::map)
(derive clojure.lang.PersistentArrayMap ::map)
(derive com.mongodb.WriteResult ::write-result)

#_(add-encoder ObjectId
             (fn [^ObjectId id ^JsonGenerator g]
               (.writeString g (str id))))

#_(extend-protocol ConvertibleToMongo
  LocalDate
  (clojure->mongo [^LocalDate d] (t/java-date d)))

#_(extend-protocol ConvertibleFromMongo
  Date
  (mongo->clojure [^Date d _kwd] (t/local-date d))

  Decimal128
  (mongo->clojure [^Decimal128 d _kwd] (.bigDecimalValue d)))

(defmulti before-save db/model-type)
(defmethod before-save :default [m] m)

(defmulti after-read db/model-type)
(defmethod after-read :default [m] m)

(defmulti prepare-criteria db/model-type)
(defmethod prepare-criteria :default [c] c)

(defn- prepare-for-put
  [m]
  (-> m
      before-save
      unqualify-keys))

(defmulti ^:private prepare-for-return (fn [x _] (type x)))
(defmethod prepare-for-return :default [x _] x)

; A WriteResult means the operation was an update
; and the source is the model submitted for update
(defmethod prepare-for-return ::write-result
  [_ source]
  (:id source))

(defmethod prepare-for-return ::map
  [after source]
  (-> after
      (rename-keys {:_id :id})
      (qualify-keys (db/model-type source)
                    :ignore #{:id})
      after-read))

(def ^:private infer-collection-name
  (comp plural
        db/model-type))

(defn- id
  [{:keys [id]}]
  (coerce-id id))

(defmulti put-model
  (fn [[op _m]] op))

(defmethod put-model ::db/insert
  [[_ m]]
  (m/insert! (infer-collection-name m)
             (prepare-for-put m)))

(defmethod put-model ::db/update
  [[_ m]]
  (m/update! (infer-collection-name m)
             {:_id (id m)}
             {:$set (-> m (dissoc :id) prepare-for-put)}))

(defmethod put-model ::db/delete
  [[_ m]]
  (m/destroy! (infer-collection-name m)
              {:_id (id m)}))

(defn- wrap-oper
  [m]
  (if (vector? m)
    m
    (if (:id m)
      [::db/update m]
      [::db/insert m])))

(defn- put*
  [conn models]
  (m/with-mongo conn
    (mapv #(-> %
               wrap-oper
               put-model
               (prepare-for-return %))
          models)))

(defn- select*
  [conn criteria options]
  (m/with-mongo conn
    (let [query (-> criteria
                    prepare-criteria
                    (criteria->query options))
          f (partial m/fetch (infer-collection-name criteria))]
      (log/debugf "fetch %s with options %s -> %s" criteria options query)
      (map #(prepare-for-return % criteria)
           (apply f (mapcat identity query))))))

(defn- delete*
  [conn models]
  (m/with-mongo conn
    (doseq [m models]
      (put-model [::db/delete m]))))

(defn- reset*
  [conn]
  (m/with-mongo conn
    (doseq [c [:users]]
      (m/destroy! c {}))))

(defn connect
  [{:keys [database username password]}]
  (m/make-connection database
                     :username username
                     :password password))

(defn- admin-conn []
  (-> (db/config :mongo)
      (assoc :database "admin"
             :username (env :mongo-adm-user)
             :password (env :mongo-adm-password))
      connect))

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

(defmethod db/reify-storage :mongo
  [config]
  (let [conn (connect config)]
    (reify
      db/Storage
      (put [_ models]       (put* conn models))
      (select [_ crit opts] (select* conn crit opts))
      (delete [_ models]    (delete* conn models))
      (close [_])
      (reset [_]            (reset* conn))
      db/StorageMeta
      (strategy-id [_] :mongo))))

