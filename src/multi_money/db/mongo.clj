(ns multi-money.db.mongo
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [config.core :refer [env]]
            [java-time.api :as t]
            [cheshire.generate :refer [add-encoder]]
            [somnium.congomongo :as m]
            [somnium.congomongo.coerce :refer [ConvertibleFromMongo
                                               ConvertibleToMongo
                                               coerce-ordered-fields]]
            [dgknght.app-lib.inflection :refer [plural]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [qualify-keys
                                      unqualify-keys]]
            [multi-money.db :as db])
  (:import java.time.LocalDate
           java.util.Date
           org.bson.types.Decimal128
           org.bson.types.ObjectId
           com.fasterxml.jackson.core.JsonGenerator))

(add-encoder ObjectId
             (fn [^ObjectId id ^JsonGenerator g]
               (.writeString g (str id))))

(extend-protocol ConvertibleToMongo
  LocalDate
  (clojure->mongo [^LocalDate d] (t/java-date d)))

(extend-protocol ConvertibleFromMongo
  Date
  (mongo->clojure [^Date d _kwd] (t/local-date d))

  Decimal128
  (mongo->clojure [^Decimal128 d _kwd] (.bigDecimalValue d)))

(defmulti coerce-id type)
(defmethod coerce-id :default [id] id)
(defmethod coerce-id String
  [id]
  (ObjectId. id))

(defn- safe-coerce-id
  [id]
  (when id (coerce-id id)))

(defmulti before-save db/model-type)
(defmethod before-save :default [m] m)

(defmulti after-read db/model-type)
(defmethod after-read :default [m] m)

(defn- prepare-for-put
  [m]
  (-> m
      unqualify-keys
      before-save))

(defn- prepare-for-return
  [after [op before]]
  (if (#{::db/insert ::db/update} op)
    (-> after
        (rename-keys {:_id :id})
        (qualify-keys (db/model-type before)
                      :ignore #{:id}))
    after))

(def ^:private infer-collection-name
  (comp plural
        db/model-type))

(defmulti put-model
  (fn [[op _m]] op))

(defmethod put-model ::db/insert
  [[_ m]]
  (m/insert! (infer-collection-name m)
             (prepare-for-put m)))

(defmethod put-model ::db/update
  [[_ m]]
  (m/update! (infer-collection-name m)
             {:_id (:id m)}
             {:$set (-> m (dissoc :id) prepare-for-put)}))

(defmethod put-model ::db/delete
  [[_ m]]
  (m/destroy! (infer-collection-name m)
              {:_id (:id m)}))

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
    (mapv #(let [with-oper (wrap-oper %)]
             (-> with-oper
               put-model
               (prepare-for-return with-oper)))
          models)))

(def ^:private oper-map
  {:> :$gt
   :>= :$gte
   :< :$lt
   :<= :$lte})

(defmulti ^:private adjust-complex-criterion
  (fn [[_k v]]
    (when (vector? v)
      (let [[oper] v]
        (or (#{:and :or} oper)
            (when (oper-map oper) :comparison)
            (first v))))))

(defn- ->mongodb-op
  [op]
  (get-in oper-map
          [op]
          (keyword (str "$" (name op)))))

(defmethod adjust-complex-criterion :default [c] c)

(defmethod adjust-complex-criterion :comparison
  [[f [op v]]]
  ; e.g. [:transaction-date [:< #inst "2020-01-01"]]
  ; ->   [:transaction-date {:$lt #inst "2020-01-01"}]
  {f {(->mongodb-op op) v}})

(defmethod adjust-complex-criterion :and
  [[f [_ & cs]]]
  {f (->> cs
          (map #(update-in % [0] ->mongodb-op))
          (into {}))})

(defmethod adjust-complex-criterion :or
  [[f [_ & cs]]]
  {f {:$or (mapv (fn [[op v]]
                   {(->mongodb-op op) v})
                 cs)}})

(defn- adjust-complex-criteria
  [criteria]
  (->> criteria
       (map adjust-complex-criterion)
       (into {})))

(defn apply-criteria
  [query criteria]
  (if (seq criteria)
    (assoc query :where (-> criteria
                            (update-in-if [:id] coerce-id)
                            (rename-keys {:id :_id})
                            adjust-complex-criteria))
    query))

(defn apply-account-id
  [{:keys [where] :as query} {:keys [account-id]}]
  (if-let [id (safe-coerce-id account-id)]
    (let [c {:$or
             [{:debit-account-id id}
              {:credit-account-id id}]}]
      (assoc query :where (if where
                            {:$and [where c]}
                            c)))
    query))

(defmulti ^:private ->mongodb-sort
  (fn [x]
    (when (vector? x)
      :explicit)))

(defmethod ->mongodb-sort :default
  [x]
  [x 1])

(defmethod ->mongodb-sort :explicit
  [sort]
  (update-in sort [1] #(if (= :asc %) 1 -1)))

(defn apply-options
  [query {:keys [limit order-by]}]
  (cond-> query
    limit (assoc :limit limit)
    order-by (assoc :sort (coerce-ordered-fields (map ->mongodb-sort order-by)))))

(defn- select*
  [conn criteria options]
  (m/with-mongo conn
    (let [query (-> {}
                    (apply-criteria (dissoc criteria :account-id))
                    (apply-account-id criteria)
                    (apply-options options))
          f (partial m/fetch (infer-collection-name criteria))]
      (log/debugf "fetch %s with options %s -> %s" criteria options query)
      (map (comp after-read
                 #(rename-keys % {:_id :id})
                 #(db/model-type % criteria))
           (apply f (mapcat identity query))))))

(defn- delete*
  [conn models]
  (m/with-mongo conn
    (let [coll-name (infer-collection-name (first models))]
      (doseq [query (map (comp #(hash-map :_id %)
                               :id)
                         models)]
        (log/debugf "delete %s" query)
        (m/destroy! coll-name query)))))

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

(defn init []
  (m/with-mongo (-> (db/config :mongo)
                    (assoc :database "admin"
                           :username (env :mongo-adm-user)
                           :password (env :mongo-adm-password))
                    connect)
    (let [roles (doto (com.mongodb.BasicDBObject.)
                  (.append "role" "readWrite")
                  (.append "db" (env :mongo-database)))
          cmd (doto (com.mongodb.BasicDBObject.)
                (.append "createUser" (env :mongo-app-user))
                (.append "pwd" (env :mongo-app-password))
                (.append "roles" (to-array [roles])))]
      (m/with-db (env :mongo-database)
        (pprint {::init (m/command! cmd)})))))

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

