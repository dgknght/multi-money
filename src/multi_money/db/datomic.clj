(ns multi-money.db.datomic
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d-peer]
            [datomic.client.api :as d-client]
            [multi-money.db.datomic.types :refer [coerce-id
                                                  ->storable]]
            [multi-money.datalog :as dtl]
            [multi-money.util :refer [+id
                                      apply-sort
                                      split-nils]]
            [multi-money.db :as db]
            [multi-money.db.datomic.tasks :as tsks]))

(derive clojure.lang.PersistentVector ::vector)
(derive clojure.lang.PersistentArrayMap ::map)
(derive clojure.lang.PersistentHashMap ::map)

(derive :datomic/peer :datomic/service)
(derive :datomic/client :datomic/service)

(defprotocol DatomicAPI
  (transact [this tx-data options])
  (query [this arg-map])
  (reset [this]))

(defn- conj* [& args]
  (apply (fnil conj []) args))

(defn apply-id
  [query {:keys [id]}]
  (if id
    (-> query
        (update-in [:args] conj* (coerce-id id))
        (update-in [:query :in] conj* '?x))
    query))

(defmulti bounding-where-clause
  (fn [crit-or-model-type]
    (if (keyword? crit-or-model-type)
      crit-or-model-type
      (db/model-type crit-or-model-type))))

(defn- unbounded-query?
  [{{:keys [in where]} :query}]
  (and (empty? where)
       (not-any? #(= '?x %) in)))

(defn- ensure-bounded-query
  [query criteria]
  (if (unbounded-query? query)
    (assoc-in query [:query :where] [(bounding-where-clause criteria)])
    query))

(defn- exclude-deleted
  [query _opts]
  (update-in query [:query :where] conj* '(not [?x :model/deleted? true])))

(defn- criteria->query
  [criteria opts]
  (let [m-type (or (db/model-type criteria)
                   (:model-type opts))]
    (-> '{:query {:find [(pull ?x [*])]
                  :in [$]}
          :args []}
        (apply-id criteria)
        (dtl/apply-criteria (dissoc criteria :id)
                            :model-type m-type
                            :query-prefix [:query]
                            :coerce ->storable)
        (ensure-bounded-query criteria)
        (exclude-deleted opts)
        (dtl/apply-options opts :model-type m-type))))

(defmulti deconstruct db/model-type)
(defmulti before-save db/model-type)
(defmulti after-read db/model-type)
(defmulti prepare-criteria db/model-type)

(defmethod deconstruct :default [m] m)
(defmethod before-save :default [m] m)
(defmethod after-read :default [m] m)
(defmethod prepare-criteria :default [c] c)

(defmulti ^:private prep-for-put type)

(defmethod prep-for-put ::map
  [m]
  (let [[m* nils] (split-nils m)]
    (cons (-> m*
              before-save
              (rename-keys {:id :db/id}))
          (->> nils
               (remove #(nil? (-> m meta :original %)))
               (map #(vector :db/retract (:id m) %))))))

#_(def ^:private action-map
  {::db/delete :db/retract
   ::db/put    :db/add})

; Here we expact that the datomic transaction has already been constructed
; like the following:
; [:db/add model-id :user/given-name "John"]
;
; Potentially, it could also look like
; [::db/delete {:id 1 :user/given-name "John"}]
; in which case we want to turn it into
; [:db/retractEntity 1]
(defmethod prep-for-put ::vector
  [[_action :as args]]
  ; For now, let's assume a deconstruct fn has prepared a legal datomic transaction
  [args])

(defn- put*
  [models {:keys [api]}]
  (let [prepped (->> models
                     (map #(+id % (comp str random-uuid)))
                     (mapcat deconstruct)
                     (mapcat prep-for-put)
                     vec)
        {:keys [tempids]} (transact api prepped {})]
    (map #(or (tempids (:db/id %))
              (:db/id %))
         prepped)))

; It seems that after an entire entity has been retracted, the id
; can still be returned
(def ^:private naked-id?
  (every-pred map?
              #(= 1 (count %))
              #(= :db/id (first (keys %)))))

(defn- extract-ref-ids
  "When datomic returns a reference to another entity, it looks like
  {:db/id <id-value>}. We want to extract the <id-value> part."
  [m]
  (->> m
       (map #(update-in %
                        [1]
                        (fn [v]
                          (if (naked-id? v)
                            (:db/id v)
                            v))))
       (into {})))

(defn- select*
  [criteria options {:keys [api]}]
  (let [qry (criteria->query criteria options)
        raw-result (query api qry)]
    (->> raw-result
         (map first)
         (remove naked-id?)
         (map (comp after-read
                    #(rename-keys % {:db/id :id})
                    extract-ref-ids))
         (apply-sort options))))

(defn- delete*
  [models {:keys [api]}]
  (transact api
            (mapv #(vector :db/add (:id %) :model/deleted? true)
                  models)
            {}))

(defmulti init-api ::db/provider)

(defmethod init-api :datomic/peer
  [{:keys [uri] :as config}]
  (let [conn (d-peer/connect uri)]
    (reify DatomicAPI
      (transact [_ tx-data options]
        @(apply d-peer/transact
                conn
                tx-data
                (mapcat identity options)))
      (query [_ {:keys [query args]}]
        ; TODO: take in the as-of date-time
        (apply d-peer/q
               query
               (cons (d-peer/db conn) args)))
      (reset [_]
        (d-peer/delete-database uri)
        (d-peer/create-database uri)
        (tsks/apply-schema config {:suppress-output? true})))))

(defmethod init-api :datomic/client
  [{:as config :keys [db-name]}]
  (let [client (d-client/client config)
        conn (d-client/connect client {:db-name db-name})]
    (reify DatomicAPI
      (transact [_ tx-data options]
        (apply d-client/transact
               conn
               {:tx-data tx-data}
               (mapcat identity options)))
      (query [_ {:keys [query args]}]
        ; TODO: take in the as-of date-time
        (apply d-client/q
               query
               (cons (d-client/db conn) args)))
      (reset [_]
        (d-client/delete-database client {:db-name db-name})
        (d-client/create-database client {:db-name db-name})
        (d-client/transact (d-client/connect client {:db-name db-name})
                                                   {:tx-data (tsks/schema)})))))

(defmethod db/reify-storage :datomic/service
  [config]
  (let [api (init-api config)]
    (reify db/Storage
      (put [_ models]       (put* models {:api api}))
      (select [_ crit opts] (select* crit opts {:api api}))
      (delete [_ models]    (delete* models {:api api}))
      (close [_])
      (reset [_]            (reset api)))))
