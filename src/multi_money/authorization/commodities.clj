(ns multi-money.authorization.commodities
  (:require [dgknght.app-lib.authorization :as auth]
            [multi-money.models.entities :as ents]
            [multi-money.util :refer [->id]]))

(defn- owner?
  [entity authenticated]
  (= (->id authenticated)
     (->id (:entity/owner entity))))

(defmethod auth/allowed? [:commodity ::auth/manage]
  [commodity _action authenticated]
  (-> commodity
      (ents/realize :commodity/entity)
      (owner? authenticated)))

(defmethod auth/scope :commodity
  [_model-type authenticated]
  {[:commodity/entity :entity/owner] authenticated})
