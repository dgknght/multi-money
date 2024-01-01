(ns multi-money.test-context
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.models.users :as usrs]))

(defonce ^:dynamic *context* nil)

(def basic-context
  {:users [#:user{:email "john@doe.com"
                  :given-name "John"
                  :surname "Doe"}]})

(defn- find-model
  [coll k v]
  (->> coll
       (filter #(= v (get-in % [k])))
       first))

(defn find-user
  ([email] (find-user email *context*))
  ([email {:keys [users]}]
   (find-model users :email email)))

(defn- put-with
  [m f]
  (or (f m)
      (pprint {::unable-to-create m})))

(defn- throw-on-failure
  [model-type]
  (fn [m]
    (or m
        (throw (RuntimeException. (format "Unable to create the %s" model-type))))))

(defn- realize-user
  [user _ctx]
  (put-with user usrs/put))

(defn- realize-users
  [ctx]
  (update-in ctx [:users] (fn [users]
                            (mapv (comp (throw-on-failure "user")
                                        #(realize-user % ctx))
                                  users))))

(defn realize
  [ctx]
  (-> ctx
      realize-users))

(defmacro with-context
  [& [a1 :as args]]
  (let [[ctx & body] (if (symbol? a1)
                       args
                       (cons basic-context args))]
    `(binding [*context* (realize ~ctx)]
       ~@body)))
