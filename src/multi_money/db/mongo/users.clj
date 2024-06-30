(ns multi-money.db.mongo.users
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db.mongo :as m]))

(defmethod m/before-save :user
  [user]
  (update-in user [:user/identities] #(mapv (fn [[p id]]
                                              {:oauth-provider (name p)
                                               :oauth-id id})
                                            %)))

(defmethod m/after-read :user
  [user]
  (update-in user [:user/identities] #(->> %
                                           (map (juxt (comp keyword :user/oauth-provider)
                                                      :user/oauth-id))
                                           (into {}))))
