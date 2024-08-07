(ns multi-money.db
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :refer [pprint]]
            [clojure.set :refer [union]]
            [config.core :refer [env]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :as utl :refer [valid-id?
                                              update-in-criteria]]))

(def comparison-opers #{:< :<= :> :>=})
(def set-opers #{:and :or})
(def opers (union comparison-opers set-opers))
(def oper? (partial contains? opers))

(s/def ::id valid-id?)

(s/def ::offset integer?)
(s/def ::limit integer?)
(s/def ::order-by vector?) ; TODO: flesh out this spec
(s/def ::count boolean?)
(s/def ::options (s/keys :opt-un [::offset ::limit ::order-by ::count]))

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
  (let [c (resolve-config-refs (get-in env [:db :strategies k]))]
    (assert c (format "No db strategy configured for %s" k))
    (assert (::provider c) (format "The configuration for %s is missing the provider key." k))
    c))

(defn storage []
  (or *storage*
      (let [active-key (get-in env [:db :active])]
        (-> env
            (get-in [:db :strategies active-key])
            resolve-config-refs
            reify-storage))))

(declare model-type)

(defn type-dispatch
  [x & _]
  (model-type x))

(defn- extract-model-type
  [m-or-t]
  (if (keyword? m-or-t)
   m-or-t
    (model-type m-or-t)))

(defn- namespaces
  "Given a criteria (map or vector containing an operator an maps) return
   all of the namespaces from the map keys in a set."
  [x]
 (cond
   (map? x) (->> (keys x)
                 (map namespace)
                 (filter identity)
                 (map keyword)
                 set)
   (sequential? x) (->> x
                        (map namespaces)
                        (reduce union))) )

(defn- single-ns
  "Give a criteria (map or vector), return the single namespace if
  only one namespace is present. Otherwise, return nil."
  [x]
  (let [namespaces (namespaces x)]
    (when (= 1 (count namespaces))
      (first namespaces))))

(defn model-type
  "The 1 arity retrieves the type for the given model. The 2 arity sets
  the type for the given model in the meta data. The 2nd argument is either a
  key identyfying the model type, or another model from which the type is to be
  extracted"
  ([m]
   (or (-> m meta ::type)
       (single-ns m)))
  ([m model-or-type]
   (vary-meta m assoc ::type (extract-model-type model-or-type))))

(defn +model-type
  [m-type]
  #(model-type % m-type))

(defn set-meta
  [m]
  (vary-meta m assoc
               :original (with-meta m nil)))

(defn changed?
  [m]
  (not= m (-> m meta ::original)))

(defn model-or-ref?
  [x]
  (and (map? x)
       (contains? x :id)))

(defn model-ref?
  [m]
  (and (map? m)
       (= #{:id} (set (keys m)))))

(defn ->model-ref
  ([x] (->model-ref x identity))
  ([x coerce]
   (if (map? x)
     (-> x
         (select-keys [:id])
         (update-in-if [:id] coerce))
     {:id (coerce x)})))

(defmulti normalize-model-ref type)

(defmethod normalize-model-ref ::utl/map
  [m]
  (select-keys m [:id]))

(defmethod normalize-model-ref :default
  [id]
  {:id id})

(defn normalize-model-refs
  "Given a criteria (map or vector) when a model is used as a value,
  replace it with a map that only conatins the :id attribute."
  [criteria]
  (->> #{:user/identity
         :entity/owner
         :commodity/entity}
       (reduce #(update-in-criteria %1 [%2] normalize-model-ref)
               criteria)))

(defmacro with-db
  [bindings & body]
  `(let [storage# (reify-storage ~(first bindings))]
     (try
       (binding [*storage* storage#]
         ~@body)
       (finally
         (close storage#)))))
