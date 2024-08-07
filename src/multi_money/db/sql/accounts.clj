(ns multi-money.db.sql.accounts
  (:require [clojure.set :refer [rename-keys]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :refer [->model-ref]]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :account [_]
  [:id :name :entity-id :commodity-id :parent-id :type])

(defn- sqlize-ids
  [m]
  (-> m
      (rename-keys {:account/entity    :account/entity-id
                    :account/commodity :account/commodity-id
                    :account/parent    :account/parent-id})
      (update-in-if [:account/entity-id]    sql/->id)
      (update-in-if [:account/commodity-id] sql/->id)
      (update-in-if [:account/parent-id]    sql/->id)))

(defn- generalize-ids
  [m]
  (-> m
      (rename-keys {:account/entity-id    :account/entity
                    :account/commodity-id :account/commodity
                    :account/parent-id    :account/parent})
      (update-in    [:account/entity]    ->model-ref)
      (update-in    [:account/commodity] ->model-ref)
      (update-in-if [:account/parent]    ->model-ref)))

(defmethod sql/prepare-criteria :account
  [criteria]
  (sqlize-ids criteria))

(defmethod sql/before-save :account
  [account]
  (-> account
      (update-in [:account/type] name)
      sqlize-ids))

(defmethod sql/after-read :account
  [account]
  (-> account
      generalize-ids
      (update-in [:account/type] keyword)))
