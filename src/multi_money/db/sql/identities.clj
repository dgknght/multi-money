(ns multi-money.db.sql.identities
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db.sql :as sql]))

(defmethod sql/attributes :identity [_]
  [:id :provider :provider-id :user-id])

(defmethod sql/before-save :identity
  [ident]
  (update-in ident [:identity/provider] name))

(defmethod sql/after-read :identity
  [ident]
  (update-in ident [:identity/provider] keyword))
