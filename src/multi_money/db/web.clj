(ns multi-money.db.web
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [config.core :refer [env]]
            [multi-money.db :refer [with-db] :as db]))

(defn- parse-db-strategy
  [s]
  (when s
    (let [[k n] (reverse (str/split s #"_"))]
      (keyword n k))))

(defn- extract-db-strategy
  [req]
  (or
    (some (comp parse-db-strategy #(get-in req %))
          [[:headers "db-strategy"]
           [:cookies "db-strategy" :value]])
    (get-in env [:db :active])))

(defn- mask-values
  [m ks]
  (reduce (fn [res k]
            (if (contains? res k)
              (assoc res k "****************")
              res))
          m
          ks))

(defn wrap-db
  [handler]
  (fn [req]
    (let [storage-key (extract-db-strategy req)
          storage-config (db/config storage-key)]
      (log/debugf "Handling request with db strategy %s -> %s"
                  storage-key
                  (mask-values storage-config [:username :user :password]))
      (with-db [storage-config]
        (handler (assoc req :db-strategy storage-key))))))
