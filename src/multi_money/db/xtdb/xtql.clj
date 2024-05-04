(ns multi-money.db.xtdb.xtql
  (:require [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]
            [multi-money.util :as utl]))

(defn extract-puts
  [models]
  (->> models
       (map (fn [m]
              [(keyword (utl/qualifier m))
               (-> m
                   utl/unqualify-keys
                   utl/+id
                   (rename-keys {:id :xt/id}))]))
       (group-by first)
       (map (comp (fn [[q ms]]
                    (into [:put-docs
                           {:into q}]
                          ms))
                  (fn [e]
                       (update-in e [1] #(map second %)))))))
