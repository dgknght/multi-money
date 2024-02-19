(ns multi-money.db.mongo.users
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
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

(defmethod m/prepare-criteria :user
  [criteria]
  (update-in-if criteria [:user/identities] (fn [[oper [provider id]]]
                                              (when-not (= := oper)
                                                (throw (RuntimeException. "Identities can only be queried by equality")))
                                              {:$elemMatch {:provider (name provider)
                                                            :id id}})))
