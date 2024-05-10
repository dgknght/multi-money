(ns multi-money.authorization.entities
  (:require [dgknght.app-lib.authorization :as auth]))

(defmethod auth/scope :entity
  [_model-type authenticated]
  {:entity/owner-id (:id authenticated)})
