(ns multi-money.authorization.entities
  (:require [dgknght.app-lib.authorization :as auth]
            [multi-money.util :refer [->id]]))

(defmethod auth/allowed? [:entity ::auth/manage]
  [{:entity/keys [owner]} _action authenticated]
  (= (->id owner) (->id authenticated)))

(defmethod auth/scope :entity
  [_model-type authenticated]
  {:entity/owner (:id authenticated)})
