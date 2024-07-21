(ns multi-money.util
  (:require [clojure.walk :refer [prewalk
                                  postwalk]]
            [clojure.string :as string]
            [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            #?(:clj [clojure.pprint :refer [pprint]]
               :cljs [cljs.pprint :refer [pprint]])
            #?(:cljs [cljs-time.core :as t])
            #?(:cljs [cljs-time.coerce :as tc]))
  #?(:clj (:import java.time.LocalDate)
     :cljs (:import goog.date.Date)))

#?(:cljs (extend-type Date
            IComparable
            (-compare [d1 d2]
               (Date/compare d1 d2))))

(defn ->storable-date
  [d]
  #?(:clj (.toEpochDay d)
     :cljs (tc/to-long (t/date-time (t/year d) (t/month d) (t/day d)))))

(defn <-storable-date
  [s]
  #?(:clj (LocalDate/ofEpochDay s)
     :cljs (tc/to-local-date s)))
 
; (def local-date?
;   #?(:clj (partial instance? LocalDate)
;      :cljs #(throw (js/Error "Not implemented"))))

(defn- key-value-tuple?
  [x]
  (and (vector? x)
       (= 2 (count x))
       (keyword? (first x))))

(defmulti qualify-key
  (fn [x & _]
    (when (key-value-tuple? x)
      :tuple)))

(defmethod qualify-key :default
  [x & _]
  x)

(defmethod qualify-key :tuple
  [[k :as x] nspace {:keys [ignore?]}]
  (if (ignore? k)
    x
    (update-in x [0] #(keyword nspace (name %)))))

(defn qualify-keys
  "Creates fully-qualified entity attributes by applying
  the :model-type from the meta data to the keys of the map."
  [m ns-key & {:keys [ignore]}]
  {:pre [(map? m)]}
  (let [k (if (keyword? ns-key)
            (name ns-key)
            ns-key)
        ignore? (if ignore
                  (some-fn ignore namespace)
                  namespace)]
    (prewalk #(qualify-key % k {:ignore? ignore?})
             m)))

(defn unqualify-keys
  "Replaces qualified keys with the simple values"
  [m]
  (prewalk (fn [x]
             (if (key-value-tuple? x)
               (update-in x [0] (comp keyword name))
               x))
           m))

(defn qualifier
  "Give a map, returns the namespace from the keys"
  [m]
  {:pre [(map? m)]}
  (let [n (->> (keys m)
               (map namespace)
               (filter identity)
               (into #{}))]
    (assert (= 1 (count n))
            "The map contains more than one keyword namespace, so the qualifier cannot be inferred.")
    (first n)))

(defmulti prepend
  (fn [coll _]
    {:pre [(sequential? coll)]}
    (cond
      (vector? coll) :vector
      (list? coll)   :list)))

(defmethod prepend :vector
  [coll v]
  (vec (concat [v] coll)))

(defmethod prepend :list
  [coll v]
  (conj coll v))

(defn- normalize-sort-key
  [x]
  (if (vector? x)
    (if (= 1 (count x))
      (conj x :asc)
      x)
    [x :asc]))

(defn- compare-fn
  [& ms]
  (fn [_ [k dir]]
    (let [[v1 v2] (map k ms)
          f (if (= :desc dir)
              #(compare %2 %1)
              compare)
          res (f v1 v2)]
      (if (= 0 res)
        0
        (reduced res)))))

(defn- sort-fn
  [order-by]
  {:pre [(vector? order-by)]}
  (fn [m1 m2]
    (->> order-by
         (map normalize-sort-key)
         (reduce (compare-fn m1 m2)
                 0))))

(defn apply-sort
  [{:keys [order-by]} models]
  (if order-by
    (sort (sort-fn order-by) models)
    models))

(defn split-nils
  "Given a map, return a tuple containing the map
  with all nil attributes removed in the first position
  and a vector containing the keys that had nil values
  in the second"
  [m]
  (reduce (fn [res [k v]]
            (if v
              (update-in res [0] assoc k v)
              (update-in res [1] conj k)))
          [{} []]
          m))

(def scalar?
  (complement coll?))

(def non-nil? (complement nil?))

(def valid-id?
  (every-pred non-nil?
              scalar?))

 (defn ->id
   "Given either an id or a model with an id, returns the id."
   [id-or-model]
   {:pre [(or (scalar? id-or-model)
              (map? id-or-model))]}
   (if (scalar? id-or-model)
     id-or-model
     (or (:id id-or-model)
         (when-let [k (->> (keys id-or-model)
                           (filter #(= "id" (name %)))
                           first)]
           (id-or-model k)))))

(defn id->ref
  [id]
  {:id id})

(defn exclude-self
  "Update a query to exclude the specified model, if the model
  has an :id attribute"
  [criteria {:keys [id]}]
  (if id
    (assoc criteria :id [:!= id])
    criteria))

(defn truncate
  ([s] (truncate s {}))
  ([s {:keys [length]
       :or {length 10}}]
   (if (> length (count s))
     s
     (reduce (fn [result s]
               (if (< length (+ (count result) (count s) 1))
                 (reduced (str result " " (first s)))
                 (str result " " s)))
             (string/split s #"\s+")))))

(defn truncate-html
  ([s] (truncate-html s {}))
  ([s opts]
   (when s
      [:span {:title s}
       (truncate s opts)])))

(defn update-in-criteria
  "Give a criteria, which is a map, or a vector with a conjunction in the first
  position and criteria in the remaining positions, apply the update-in f
  to all maps found by walking the data structure."
  [c k f & args]
  (prewalk #(if (map? %)
              (apply update-in-if % k f args)
              %)
           c))

(defn rename-criteria-keys
  "Give a criteria, which is a map, or a vector with a conjunction in the first
  position and criteria in the remaining positions, apply rename-keys
  to all maps found by walking the data structure."
  [c k-map]
  (prewalk #(if (map? %)
              (rename-keys % k-map)
              %)
           c))

(defn +id
  "Given a map without an :id value, adds one with a random UUID as a value"
  ([m] (+id m random-uuid))
  ([m id-fn]
   (if (:id m)
     m
     (assoc m :id (id-fn)))))

(defn mask-values
  [data & ks]
  (postwalk (fn [x]
              (if (map? x)
                (reduce (fn [m k]
                          (if (contains? m k)
                            (assoc m k "********")
                            m))
                        x
                        ks)
                x))
            data))

; When we update to the lastest version of clojurescript, I believe we can remove this
#?(:cljs (defn update-keys
           [m f]
           (postwalk (fn [x]
                       (if (map-entry? x)
                         (update-in x [0] f)
                         x))
                     m)))

(defn select-namespaced-keys
  [m ks]
  (let [n (namespace (first ks))]
    (merge (-> m
               (select-keys (map (comp keyword name) ks))
               (update-keys #(keyword n (name %))))
           (select-keys m ks))))

(defn deep-rename-keys
  [m key-map]
  (postwalk (fn [x]
              (if (map? x)
                (rename-keys x key-map)
                x))
            m))
