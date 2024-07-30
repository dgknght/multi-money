(ns multi-money.db.sql.accounts
  (:require [multi-money.db.sql :as sql]))

(defmethod sql/attributes :account [_]
  [:id :name :entity-id :commodity-id :type])
