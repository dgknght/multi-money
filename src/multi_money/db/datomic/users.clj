(ns multi-money.db.datomic.users
  (:require [clojure.pprint :refer [pprint]]
            [multi-money.db.datomic :as d]))

; reshape the identities from
; {:id 1
;  :user/identities {:google "abc123"
;                    :github "def456"}}
; to
; [:db/add 1 :user/identities [:google "abc123"]]
; [:db/add 1 :user/identities [:github "def456"]]
(defmethod d/deconstruct :user
  [{:user/keys [identities] :keys [id] :as user}]
  (cons (dissoc user :user/identities)
        (map (fn [oauth-id]
               [:db/add id :user/identities oauth-id])
             identities)))

; reshape the identities from
; {:id 1
;  :user/identities [[:google "abc123"]
;                    [:github "def456"]]}
; to
; {:id 1
;  :user/identities {:google "abc123"
;                    :github "def456"}}
(defmethod d/after-read :user
  [user]
  (update-in user
             [:user/identities]
             #(into {} %)))
