(ns multi-money.authorization.accounts
  (:require [dgknght.app-lib.authorization :as auth]
            [multi-money.models.entities :as ents]
            [multi-money.util :refer [->id]]))

(defmethod auth/allowed? [:account ::auth/manage]
  [account _action authenticated]
  (let [{{:entity/keys [owner]} :account/entity} (ents/realize account :account/entity)]
    (= (->id owner) (->id authenticated))))

(defmethod auth/scope :account
  [_model-type authenticated]
  {:entity/owner (:id authenticated)})
