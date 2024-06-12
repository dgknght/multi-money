(ns multi-money.db.mongo
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [clojure.walk :refer [postwalk]]
            [somnium.congomongo :as m]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->snake_case
                                            ->kebab-case]]
            [dgknght.app-lib.inflection :refer [plural
                                                singular]]
            [multi-money.util :as utl :refer [qualify-keys
                                              unqualify-keys]]
            [multi-money.db :as db]
            [multi-money.db.mongo.queries :refer [criteria->pipeline]]
            [multi-money.db.mongo.types :refer [coerce-id]]))

(derive clojure.lang.PersistentHashMap ::map)
(derive clojure.lang.PersistentArrayMap ::map)
(derive com.mongodb.WriteResult ::write-result)

(def ^:private relationships
  #{[:users :entities]
    [:entities :commodities]})

(defmulti before-save db/model-type)
(defmethod before-save :default [m] m)

(defmulti after-read db/model-type)
(defmethod after-read :default [m] m)

(defmulti prepare-criteria db/model-type)
(defmethod prepare-criteria :default [c] c)

(def ^:private ->mongo-keys (partial transform-keys ->snake_case))
(def ^:private ->clj-keys (partial transform-keys ->kebab-case))

(defn- prepare-for-put
  [m]
  (-> m
      before-save
      unqualify-keys
      ->mongo-keys))

(defmulti ^:private prepare-for-return (fn [x _] (type x)))
#_(defmethod prepare-for-return :default [x _] x)

; A WriteResult means the operation was an update
; and the source is the model submitted for update
(defmethod prepare-for-return ::write-result
  [_ source]
  (:id source))

(defmethod prepare-for-return ::map
  [after source]
  (-> after
      (rename-keys {:_id :id})
      ->clj-keys
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

(defn- coerce-criteria-id
  [criteria]
  (postwalk (fn [x]
              (if (and (instance? clojure.lang.MapEntry x)
                       (= :id (first x)))
                (update-in x [1] coerce-id)
                x))
            criteria))

; TODO: how much of this can be constructed from the relationships?
(def ^:private model-refs->ids
  {:entity/owner :entity/owner-id
   :commodity/entity :commodity/entity-id})

(defn- normalize-model-refs
  "Given a criteria (map or vector) when a model is used as a value,
  replace it with a map that only conatins the :id attribute."
  [criteria]
  (->> #{:user/entity
         :entity/owner
         :commodity/entity}
       (filter criteria)
       (reduce #(utl/update-in-criteria %1 [%2] (fn [v]
                                                  (if (map? v)
                                                    (select-keys v [:id])
                                                    v)))
               criteria)))

(defn- select*
  [conn criteria {:as options :keys [count]}]
  ; separate criteria by namespace
  ; put queries in order by relationship
  ; execute queries, feeding results into next
  (let [col-name (infer-collection-name criteria)
        pipeline (-> criteria
                     coerce-criteria-id
                     normalize-model-refs
                     prepare-criteria
                     (criteria->pipeline (assoc options
                                                :collection col-name
                                                :relationships relationships)))]
    (log/debugf "aggregate %s with options %s -> %s" criteria options pipeline)
    (m/with-mongo conn
      (if count
        (throw (ex-info "Not implemented" {}))
        #_(apply m/fetch-count col-name (mapcat identity query))
        (let [result (apply m/aggregate
                            col-name
                            (concat pipeline [:as :clojure]))]
          (map #(prepare-for-return % criteria)
               (:result result)))))))

(defn- delete*
  [conn models]
  (m/with-mongo conn
    (doseq [m models]
      (put-model [::db/delete m]))))

(defn- reset*
  [conn]
  (m/with-mongo conn
    (doseq [c [:users :entities :commodities]]
      (m/destroy! c {}))))

(defn connect
  [{:keys [database username password host port]
    :or {host "localhost"
         port 27017}}]
  (m/make-connection database
                     :instance {:host host :port port}
                     :username username
                     :password password))

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

