(ns multi-money.db
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :refer [pprint]]
            [clojure.set :refer [union]]
            [config.core :refer [env]]
            [multi-money.util :refer [valid-id?]]))

(def comparison-opers #{:< :<= :> :>=})
(def set-opers #{:and :or})
(def opers (union comparison-opers set-opers))
(def oper? (partial contains? opers))

(s/def ::id valid-id?)

(s/def ::offset integer?)
(s/def ::limit integer?)
(s/def ::order-by vector?) ; TODO: flesh out this spec
(s/def ::options (s/keys :opt-un [::offset ::limit ::order-by]))

; To add a new storage implemenation, add a new namespace and a new
; implementation of the multi method reify-storage, which should
; return an implemention of Storage

(defprotocol StorageMeta
  "Declares additional, information about the storage strategy"
  (strategy-id [this]))

(defprotocol Storage
  "Defines the functions necessary to provider storage for the application"
  (put [this models] "Saves the models to the database in an atomic transaction")
  (select [this criteria options] "Retrieves models from the database")
  (delete [this models] "Removes the models from the database in an atomic transaction")
  (close [this] "Releases resources held by the storage instance")
  (reset [this] "Resets the database")) ; TODO: Is there someplace to put this so it's only available in tests?

(defn storage-dispatch [config & _]
  (::provider config))

(defmulti reify-storage storage-dispatch)

(def ^:dynamic *storage* nil)

(defn- config-ref?
  [x]
  (and (keyword? x)
       (= "config" (namespace x))))

(defn- config-ref-key
  [x]
  (when (config-ref? x)
    (-> x name keyword)))

(defn- resolve-config-refs
  [config]
  (postwalk (fn [x]
              (if-let [k (config-ref-key x)]
                (env k)
                x))
            config))

(defn configs []
  (assert (seq (:db env)) "At least one db strategy must be configured")
  (map resolve-config-refs
       (get-in env [:db :strategies])))

(defn config [k]
  (resolve-config-refs (get-in env [:db :strategies k])))

(defn storage []
  (or *storage*
      (let [active-key (get-in env [:db :active])]
        (-> env
            (get-in [:db :strategies active-key])
            resolve-config-refs
            reify-storage))))

(declare model-type)

(defn- extract-model-type
  [m-or-t]
  (if (keyword? m-or-t)
   m-or-t
    (model-type m-or-t)))

(defn model-type
  "The 1 arity retrieves the type for the given model. The 2 arity sets
  the type for the given model. The 2nd argument is either a key identyfying
  the model type, or another model from which the type is to be extracted"
  ([m]
   (let [namespaces (->> (keys m)
                         (remove vector?)
                         (map namespace)
                         (filter identity)
                         (map keyword)
                         (into #{}))]
     (if (= 1 (count namespaces))
       (first namespaces)
       (-> m meta :model-type))))
  ([m model-or-type]
   (vary-meta m assoc :model-type (extract-model-type model-or-type))))

(defn +model-type
  [m-type]
  #(model-type % m-type))

(defn set-meta
  [m model-or-type]
  (vary-meta m assoc
               :model-type (extract-model-type model-or-type)
               :original (with-meta m nil)))

(defn changed?
  [m]
  (not= m (-> m meta :original)))

(defmacro with-db
  [bindings & body]
  `(let [storage# (reify-storage ~(first bindings))]
     (try
       (binding [*storage* storage#]
         ~@body)
       (finally
         (close storage#)))))
