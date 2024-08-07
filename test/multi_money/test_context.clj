(ns multi-money.test-context
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db :as db]
            [multi-money.models.users :as usrs]
            [multi-money.models.entities :as ents]
            [multi-money.models.commodities :as cdts]
            [multi-money.models.accounts :as acts]
            [multi-money.models.transactions :as trxs]))

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
                             :entity "Jane's Money"}]
   :accounts [#:account{:name "Checking"
                        :entity "Personal"
                        :type :asset}
              #:account{:name "Salary"
                        :entity "Personal"
                        :type :income}
              #:account{:name "Rent"
                        :entity "Personal"
                        :type :expense}
              #:account{:name "Groceries"
                        :entity "Personal"
                        :type :expense}
              #:account{:name "Credit Card"
                        :entity "Personal"
                        :type :liability}]})

(defn- key-value-pred
  [kvs]
  #(every? (fn [[k v]]
             (= v (get-in % [k])))
          (partition 2 (take-while identity kvs))))

(defn- detect-model
  ([coll pred]
   (->> coll
        (filter pred)
        first))
  ([coll k v & kvs]
   {:pre [(= 0
             (mod (count kvs)
                  2))]}
   (detect-model coll (key-value-pred (concat [k v] kvs)))))

(defn- find-model
  [coll k v & kvs]
  {:pre [(coll? coll)]}
  (or (apply detect-model coll k v kvs)
      (throw (ex-info "Unable to find the model" {:match (->> kvs
                                                              (concat [k v])
                                                              (partition 2)
                                                              (map vec)
                                                              (into {}))
                                                  :collection coll}))))
(defn- parse-model-ident
  [ident & [k :as ks]]
  (cond
    (map? ident)    ident
    (vector? ident) (zipmap ks ident)
    :else           {k ident}))

(defn find-user
  ([identifier] (find-user identifier *context*))
  ([identifier {:keys [users]}]
   (or (detect-model users :user/username identifier)
       (detect-model users :user/email identifier))))

(defn find-entity
  ([name] (find-entity name *context*))
  ([name {:keys [entities] :as context}]
   {:pre [(map? context)]}
   (find-model entities :entity/name name)))

(defn- resolve-entity-ref
  [ref context]
  {:pre [(map? context)
         (or (string? ref)
             (map? ref))]}
  (when ref
    (select-keys (if (string? ref)
                   (find-entity ref context)
                   ref)
                 [:id])))

(defn find-commodity
  ([ident] (find-commodity ident *context*))
  ([ident {:keys [commodities] :as context}]
   {:pre [(map? context)]}
   (let [{:keys [symbol entity-ref]} (parse-model-ident ident
                                                        :symbol
                                                        :entity-ref)
         entity (resolve-entity-ref entity-ref context)]
     (find-model commodities
                 :commodity/symbol symbol
                 :commodity/entity entity))))

(defn find-account
  ([ident] (find-account ident *context*))
  ([ident {:keys [accounts] :as context}]
   {:pre [(map? context)]}
   (let [{:keys [name entity-ref]} (parse-model-ident ident :name :entity-ref)]
     (find-model accounts
                 :account/name name
                 :account/entity (resolve-entity-ref entity-ref context)))))

(defn find-transaction
  ([ident] (find-transaction ident *context*))
  ([ident {:keys [transactions] :as context}]
   {:pre [(map? context)]}
   (let [{:keys [date description entity-ref]} (parse-model-ident ident
                                                                  :date
                                                                  :description
                                                                  :entity-ref)]
     (find-model transactions
                 :transaction/date date
                 :transaction/description description
                 :transactin/entity (resolve-entity-ref entity-ref context)))))

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
  db/type-dispatch)

(defmethod prepare-for-put :user
  [user _]
  user)

(defmethod prepare-for-put :entity
  [entity ctx]
  (update-in entity [:entity/owner] #(find-user % ctx)))

(defmethod prepare-for-put :commodity
  [commodity ctx]
  (update-in commodity [:commodity/entity] #(find-entity % ctx)))

(defmethod prepare-for-put :account
  [{:as account :account/keys [entity]} ctx]
  (let [entity (find-entity entity ctx)]
    (-> account
        (assoc :account/entity entity)
        (update-in [:account/commodity]
                   #(or (if %
                          (find-commodity [% entity] ctx)
                          (:entity/default-commodity entity))
                        (throw (ex-info "Unable to find commodity for the account" {:account account
                                                                                    :entity entity})))))))

(defn- realize-collection
  [ctx coll-key desc put-fn]
  (update-in ctx
             [coll-key]
             (fn [coll]
               (mapv (comp (throw-on-failure desc)
                           #(put-with % put-fn)
                           #(prepare-for-put % ctx))
                     coll))))

(defn- apply-default-commodities
  [{:keys [commodities] :as ctx}]
  (let [comm-map (->> commodities
                      (group-by (comp :id :commodity/entity))
                      (map #(update-in % [1] first))
                      (into {}))]
    (update-in ctx [:entities] (fn [entities]
                                 (mapv (comp
                                         ents/put
                                         #(assoc %
                                                 :entity/default-commodity
                                                 (comm-map (:id %))))
                                       entities)))))

(defn realize
  [ctx]
  (-> ctx
      (realize-collection :users "user" usrs/put)
      (realize-collection :entities "entity" ents/put)
      (realize-collection :commodities "commodity" cdts/put)
      (apply-default-commodities)
      (realize-collection :accounts "account" acts/put)
      (realize-collection :transactions "transaction" trxs/put)))

(defmacro with-context
  [& [a1 :as args]]
  (let [[ctx & body] (if (symbol? a1)
                       args
                       (cons basic-context args))]
    `(binding [*context* (realize ~ctx)]
       ~@body)))
