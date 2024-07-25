(ns multi-money.db.datomic.users
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.core :refer [update-in-if]]
            [multi-money.util :as utl]
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

(defmulti ^:private prep type)

(defmethod prep ::utl/map
  [criteria]
  (update-in-if criteria
                [:user/identities]
                (fn [v]
                  (if (and (vector? v)
                           (= :including (first v)))
                    (update-in v [1] (juxt :identity/oauth-provider
                                           :identity/oauth-id))
                    v))))

(defmethod prep ::utl/vector
  [[conj & cs]]
  (vec (cons conj (map prep cs))))

(defmethod d/prepare-criteria :user
  [criteria]
  (prep criteria))
