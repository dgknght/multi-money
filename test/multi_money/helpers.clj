(ns multi-money.helpers
  (:require [clojure.test :refer [deftest testing]]
            [clojure.pprint :refer [pprint]]
            [config.core :refer [env]]
            #_[ring.mock.request :as req]
            #_[dgknght.app-lib.test :refer [parse-json-body]]
            [multi-money.db :as db]
            #_[multi-money.tokens :as tkns]
            #_[multi-money.models.users :as usrs]
            #_[multi-money.handler :refer [app]]))

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
               #{isolate}))
(def ignore-strategy (if isolate
                       (complement isolate)
                       (->set (env :ignore-strategy))))
(def honor-strategy (complement ignore-strategy))

(defn dbs []
  (assert (seq (:db env)) "At least one db strategy must be configured")
  (get-in env [:db :strategies]))

(defn reset-db [f]
  (let [dbs (->> (get-in env [:db :strategies])
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
                                       (dbs))]
         (binding [*strategy* (keyword name#)]
           (testing (format "database strategy %s" name#)
             (db/with-db [config#]
               ~@bod)))))))

#_(defn +auth
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

#_(defn- +json-body
  [req json-body]
  (if json-body
    (req/json-body req json-body)
    req))

#_(defn request
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
