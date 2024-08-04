(ns multi-money.db.datomic
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [postwalk]]
            [datomic.api :as d-peer]
            [datomic.client.api :as d-client]
            [stowaway.datalog :refer [apply-options]]
            [multi-money.db.datomic.types :refer [coerce-id
                                                  ->storable]]
            [multi-money.datalog :as dtl]
            [multi-money.util :as utl :refer [+id
                                              apply-sort
                                              split-nils
                                              deep-rename-keys]]
            [multi-money.db :as db]
            [multi-money.db.datomic.tasks :as tsks]))

(derive :datomic/peer :datomic/service)
(derive :datomic/client :datomic/service)

(def ->id (comp coerce-id utl/->id))

(defprotocol DatomicAPI
  (transact [this tx-data options])
  (query [this arg-map])
  (reset [this]))

(defmulti bounding-where-clause
  (fn [crit-or-model-type]
    (if (keyword? crit-or-model-type)
      crit-or-model-type
      (db/model-type crit-or-model-type))))

(def ^:private not-deleted '(not [?x :model/deleted? true]))

(defn ->model-ref
  [x]
  (db/->model-ref x coerce-id))

(defn- unbounded-query?
  [{:keys [in where]}]
  (and (empty? (remove #(= not-deleted %) where))
       (not-any? #(= '?x %) in)))

(defn- ensure-bounded-query
  [query criteria]
  (if (unbounded-query? query)
    (assoc-in query [:where] [(bounding-where-clause criteria)])
    query))

(defn- rearrange-query
  "Takes a simple datalog query and adjust the attributes
  to match the format expected by datomic."
  [query]
  (-> query
      (select-keys [:args])
      (assoc :query (dissoc query :args))))

(defn- criteria->query
  [criteria {:as opts :keys [count]}]
  (let [m-type (or (db/model-type criteria)
                   (:model-type opts))]
    (-> {:find (if count
                 '[(count ?x)]
                 '[(pull ?x [*])])
         :in '[$]
         :where [not-deleted]
         :args []}
        (dtl/apply-criteria criteria
                            :target m-type
                            :coerce ->storable)
        (ensure-bounded-query criteria)
        (apply-options (dissoc opts :order-by :sort))
        rearrange-query)))

(defmulti deconstruct db/model-type)
(defmulti before-save db/model-type)
(defmulti after-read db/model-type)
(defmulti prepare-criteria db/model-type)

(defmethod deconstruct :default [m] [m])
(defmethod before-save :default [m] m)
(defmethod after-read :default [m] m)
(defmethod prepare-criteria :default [c] c)

(defmulti ^:private prep-for-put type)

(defmethod prep-for-put ::utl/map
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
(defmethod prep-for-put ::utl/vector
  [[_action :as args]]
  ; For now, let's assume a deconstruct fn has prepared a legal datomic transaction
  [args])

(defn- put*
  [models {:keys [api]}]
  {:pre [(sequential? models)]}

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

(defn- coerce-criteria-id
  [criteria]
  (postwalk (fn [x]
              (if (and (map-entry? x)
                       (= :id (first x)))
                (update-in x [1] coerce-id)
                x))
            criteria))

; TODO: Remove this, it's part of stowaway now
(defn- extract-model-ref-ids
  [criteria]
  (postwalk (fn [x]
              (if (and (map-entry? x)
                       (db/model-ref? (second x)))
                (update-in x [1] :id)
                x))
            criteria))

(defn- select*
  [criteria {:as options :keys [count]} {:keys [api]}]
  (let [qry (-> criteria
                coerce-criteria-id
                extract-model-ref-ids
                prepare-criteria
                (criteria->query options))
        raw-result (query api qry)]
    (if count
      (ffirst raw-result)
      (->> raw-result
           (map first)
           (remove naked-id?)
           (map (comp after-read
                      #(deep-rename-keys % {:db/id :id})))
           (apply-sort options)))))

(defn- delete*
  [models {:keys [api]}]
  {:pre [(and (sequential? models)
              (not-any? nil? models))]}
  (transact api
            (mapv #(vector :db/add (:id %) :model/deleted? true)
                  models)
            {}))

(defmulti init-api ::db/provider)

(defmethod init-api :datomic/peer
  [{:keys [uri] :as config}]
  (reify DatomicAPI
    (transact [_ tx-data options]
      @(apply d-peer/transact
              (d-peer/connect uri)
              tx-data
              (mapcat identity options)))
    (query [_ {:keys [query args]}]
      ; TODO: take in the as-of date-time
      (apply d-peer/q
             query
             (cons (-> uri d-peer/connect d-peer/db) args)))
    (reset [_]
      (d-peer/delete-database uri)
      (tsks/apply-schema config {:suppress-output? true}))))

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
        ; probably should not ever get here, as this is for unit tests only
        ))))

(defmethod db/reify-storage :datomic/service
  [config]
  (let [api (init-api config)]
    (reify db/Storage
      (put [_ models]       (put* models {:api api}))
      (select [_ crit opts] (select* crit opts {:api api}))
      (delete [_ models]    (delete* models {:api api}))
      (close [_])
      (reset [_]            (reset api)))))
