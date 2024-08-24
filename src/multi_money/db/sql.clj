(ns multi-money.db.sql
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [next.jdbc :as jdbc]
            [next.jdbc.plan :refer [select!
                                    select-one!]]
            [next.jdbc.sql.builder :refer [for-insert
                                           for-update
                                           for-delete]]
            [stowaway.criteria :as crt]
            [dgknght.app-lib.core :refer [update-in-if]]
            [dgknght.app-lib.inflection :refer [plural]]
            [multi-money.util :as utl]
            [multi-money.db.sql.queries :refer [criteria->query]]
            [multi-money.db.sql.types :refer [coerce-id
                                              temp-id?]]
            [multi-money.db :as db]))

(defn- id
  [{:keys [id]}]
  (coerce-id id))

(def ->id
  (comp coerce-id utl/->id))

(defmulti before-save
  "Called before insert/update so that values can be adjusted
  from clojure idioms to SQL idioms"
  db/model-type)
(defmethod before-save :default [m] m)

(defmulti deconstruct
  "Called on a model before before-save so that complex objects
  can be extracted prior to insert/update"
  db/model-type)
(defmethod deconstruct :default [m] [m])

(def ^:private infer-table-name
  (comp keyword
        plural
        utl/qualifier))

(defn- insert
  [db model]
  (let [table (infer-table-name model)
        s (for-insert table
                      model
                      jdbc/snake-kebab-opts)
        result (jdbc/execute-one! db s {:return-keys [:id]})]

    ; TODO: scrub for sensitive data
    (log/debugf "database insert %s -> %s" model s)
    (get-in result [(keyword (name table) "id")])))

(defn- update
  [db model]
  {:pre [(:id model)]}
  (let [table (infer-table-name model)
        s (for-update table
                      (dissoc model :id)
                      {:id (id model)}
                      jdbc/snake-kebab-opts)
        result (jdbc/execute-one! db s {:return-keys [:id]})]

    ; TODO: scrub sensitive data
    (log/debugf "database update %s -> %s" model s)

    (get-in result [(keyword (name table) "id")])))


(defmulti after-read db/model-type)
(defmethod after-read :default [m] m)

(defmulti attributes identity)

(defmulti prepare-criteria db/model-type)
(defmethod prepare-criteria :default [m] m)

(defn delete-one
  [db m]
  (let [s (for-delete (infer-table-name m)
                      {:id (:id m)} ; TODO: find the id attribute
                      {})]

    ; TODO: scrub sensitive data
    (log/debugf "database delete %s -> %s" m s)

    (jdbc/execute! db s)
    1))

(defn- wrap-oper
  [m]
  (if (vector? m)
    m
    [(if (and (:id m) ; TODO: Change this to find the id attribute
              (not (temp-id? m)))
       ::db/update
       ::db/insert)
     m]))

(defn- put-one
  [db [oper model]]
  (case oper
    ::db/insert (insert db model)
    ::db/update (update db model)
    ::db/delete (delete-one db model)))

; this is not unlike the way datomic handles temporary ids
; if saving multiple models that need to reference each other
; before the database has issued an ID, we can specify temporary
; ids that will be resolved as needed during the save process
(defmulti resolve-temp-ids
  (fn [model _id-map]
    (db/model-type model)))

(defmethod resolve-temp-ids :default
  [model _id-map]
  model)

(defn- execute-and-aggregate
  [db {:as result :keys [id-map]} [operator m]]
  (let [ready-to-save (cond-> m
                        (seq id-map) (resolve-temp-ids id-map)
                        (temp-id? m) (dissoc :id))
        saved (put-one db [operator ready-to-save])]
    (cond-> (update-in result [:saved] conj saved)
      (temp-id? m)
      (assoc-in [:id-map (:id m)]
                saved))))

(defn put*
  [db models]
  ; TODO: refactor this to handle temporary ids
  (jdbc/with-transaction [tx db]
    (:saved (->> models
                 (mapcat deconstruct)
                 (map (comp wrap-oper
                            before-save))
                 (reduce (partial execute-and-aggregate tx)
                         {:saved []})))))

(defn- id-key
  [x]
  (when-let [target (db/model-type x)]
    (keyword (name target) "id")))

(defn- massage-ids
  "Coerces ids and appends the appropriate namespace
  to the :id key"
  [m]
  (let [k (id-key m)]
    (cond-> (utl/update-in-criteria m [:id] coerce-id)
      k (rename-keys {:id k}))))

(defn- select*
  [db criteria options]
  (let [query (-> criteria
                  (crt/apply-to massage-ids)
                  prepare-criteria
                  (criteria->query (assoc options
                                          :target (db/model-type criteria))))]

    ; TODO: scrub sensitive data
    (log/debugf "database select %s with options %s -> %s" criteria options query)

    (if (:count options)
      (select-one! db
                   :record-count
                   query
                   jdbc/unqualified-snake-kebab-opts)
      (let [q (db/model-type criteria)]
        (map (comp after-read
                   #(utl/qualify-keys % q :ignore #{:id}))
             (select! db
                      (attributes q)
                      query
                      jdbc/snake-kebab-opts))))))

(defn- delete*
  [db models]
  (jdbc/with-transaction [tx db]
    (->> models
         (map (comp #(put-one tx %)
                    #(vector ::db/delete %)
                    #(update-in % [:id] coerce-id)))
         (reduce +))))

(defn- reset*
  [db]
  (jdbc/execute! db ["truncate table users cascade"]))

(defmethod db/reify-storage :sql
  [config]
  {:pre [(:user config)
         (:password config)]}
  (let [db (jdbc/get-datasource config)]
    (reify
      db/Storage
      (put [_ models]       (put* db models))
      (select [_ crit opts] (select* db crit opts))
      (delete [_ models]    (delete* db models))
      (close [_])
      (reset [_]            (reset* db))
      db/StorageMeta
      (strategy-id [_] :sql))))

(defmacro def->sql-refs
  [fn-name & keys]
  (let [id-keys (mapv #(keyword (namespace %)
                                (str (name %) "-id"))
                      keys)
        key-map (zipmap keys id-keys)]
    `(defn- ~fn-name
       [model#]
       (reduce #(update-in-if %1 [%2] ->id)
               (rename-keys model# ~key-map)
               ~id-keys))))
