(ns multi-money.helpers
  (:require [clojure.test :refer [deftest testing]]
            [clojure.pprint :refer [pprint]]
            [config.core :refer [env]]
            [ring.mock.request :as req]
            [dgknght.app-lib.test :refer [parse-json-body]]
            [multi-money.db :as db]
            [multi-money.tokens :as tkns]
            [multi-money.models.users :as usrs]
            [multi-money.handler :refer [app]]))

(def ^:dynamic *strategy* nil)

(defn ->set
  [v]
  (if (coll? v)
    (set v)
    #{v}))

(defn include-strategy
  [{:keys [only exclude]}]
  (cond
    only    (list 'multi-money.helpers/->set only)
    exclude `(complement ~(->set exclude))
    :else   '(constantly true)))

(def isolate (when-let [isolate (env :isolate)]
               #{(if (string? isolate)
                   (keyword isolate)
                   isolate)}))

(def ignore-strategy (if isolate
                       (complement isolate)
                       (->set (env :ignore-strategy))))

(def honor-strategy (complement ignore-strategy))

(defn reset-db [f]
  (let [dbs (->> (db/configs)
                 (remove (comp ignore-strategy first))
                 vals
                 (mapv db/reify-storage))]
    (doseq [db dbs]
      (db/reset db))
    (f)))

(defn ensure-opts
  [args]
  (if (map? (first args))
    args
    (cons {} args)))

(defmacro dbtest
  [test-name & body]
  (let [[opts & bod] (ensure-opts body)]
    `(deftest ~test-name
       (doseq [[name# config#] (filter (comp (every-pred ~(include-strategy opts)
                                                         honor-strategy)
                                             first)
                                       (db/configs))]
         (binding [*strategy* (keyword name#)]
           (testing (format "database strategy %s" name#)
             (db/with-db [config#]
               ~@bod)))))))

(defn +auth
  [rq user & [user-agent]]
  (if user
    (req/header rq
                "Authorization"
                (format "Bearer %s" (-> user
                                        usrs/tokenize
                                        (assoc :user-agent (or user-agent
                                                               "test-user-agent"))
                                        tkns/encode)))
    rq))

(defn- +json-body
  [req json-body]
  (if json-body
    (req/json-body req json-body)
    req))

(defn request
  [method
   path
   & {:keys [header-user-agent
             tokenized-user-agent
             json-body
             user]
      :or {tokenized-user-agent "test-user-agent"
           header-user-agent "test-user-agent"}}]
  (-> (req/request method path)
      (req/header "user-agent" header-user-agent)
      (+auth user tokenized-user-agent)
      (+json-body json-body)
      app
      parse-json-body))
