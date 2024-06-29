(ns multi-money.db.mongo.users
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db.mongo :as m]))

(defmethod m/before-save :user
  [user]
  (update-in user [:user/identities] #(mapv (fn [[p id]]
                                              {:provider (name p)
                                               :id id})
                                            %)))

(defmethod m/after-read :user
  [user]
  (update-in user [:user/identities] #(->> %
                                           (map (juxt (comp keyword :user/provider)
                                                      :id))
                                           (into {}))))
