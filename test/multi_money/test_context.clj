(ns multi-money.test-context
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db :as db]
            [multi-money.models.users :as usrs]
            [multi-money.models.entities :as ents]
            [multi-money.models.commodities :as cdts]))

(defonce ^:dynamic *context* nil)

(def basic-context
  {:users [#:user{:email "john@doe.com"
                  :given-name "John"
                  :surname "Doe"
                  :identities {:google "abc123"}}
           #:user{:email "jane@doe.com"
                  :given-name "Jane"
                  :surname "Doe"
                  :identities {:google "def456"}}]
   :entities [#:entity{:name "Personal"
                       :owner "john@doe.com"}
              #:entity{:name "Business"
                       :owner "john@doe.com"}
              #:entity{:name "Jane's Money"
                       :owner "jane@doe.com"}]
   :commodities [#:commodity{:symbol "USD"
                             :name "United States Dollar"
                             :type :currency
                             :entity "Personal"}
                 #:commodity{:symbol "CAD"
                             :name "Candadian Dollar"
                             :type :currency
                             :entity "Jane's Money"}]})

(defn- find-model
  [coll k v]
  (->> coll
       (filter #(= v (get-in % [k])))
       first))

(defn find-user
  ([identifier] (find-user identifier *context*))
  ([identifier {:keys [users]}]
   (or (find-model users :user/username identifier)
       (find-model users :user/email identifier))))

(defn find-entity
  ([name] (find-entity name *context*))
  ([name {:keys [entities]}]
   (find-model entities :entity/name name)))

(defn find-commodity
  ([symbol] (find-commodity symbol *context*))
  ([symbol {:keys [commodities]}]
   (find-model commodities :commodity/symbol symbol)))

(defn- put-with
  [m f]
  (or (f m)
      (pprint {::unable-to-create m})))

(defn- throw-on-failure
  [model-type]
  (fn [m]
    (or m
        (throw (RuntimeException. (format "Unable to create the %s" model-type))))))

(defmulti ^:private prepare-for-put
  (fn [m _ctx] (db/model-type m)))

(defmethod prepare-for-put :default [m _] m)

(defmethod prepare-for-put :entity
  [entity ctx]
  (update-in entity [:entity/owner] #(find-user % ctx)))

(defmethod prepare-for-put :commodity
  [commodity ctx]
  (update-in commodity [:commodity/entity] #(find-entity % ctx)))

(defn- realize-collection
  [ctx coll-key desc put-fn]
  (update-in ctx
             [coll-key]
             (fn [coll]
               (mapv (comp (throw-on-failure desc)
                           #(put-with % put-fn)
                           #(prepare-for-put % ctx))
                     coll))))
(defn realize
  [ctx]
  (-> ctx
      (realize-collection :users "user" usrs/put)
      (realize-collection :entities "entity" ents/put)
      (realize-collection :commodities "commodity" cdts/put)))

(defmacro with-context
  [& [a1 :as args]]
  (let [[ctx & body] (if (symbol? a1)
                       args
                       (cons basic-context args))]
    `(binding [*context* (realize ~ctx)]
       ~@body)))
