(ns multi-money.models.users-test
  (:require [clojure.test :refer [is use-fixtures testing]]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [java-time.api :as t]
            [java-time.mock :refer [mock-clock]]
            [java-time.clock :refer [with-clock]]
            [multi-money.helpers :refer [reset-db
                                         dbtest]]
            [multi-money.test-context :refer [with-context
                                              find-user]]
            [multi-money.models.users :as usrs]
            [multi-money.db.mongo.ref]
            [multi-money.db.sql.ref]
            #_[multi-money.db.xtdb.ref]
            #_[multi-money.db.datomic.ref]))

(use-fixtures :each reset-db)

(def attr
  #:user{:email "john@doe.com"
         :given-name "John"
         :surname "Doe"})

(dbtest create-a-user
  (let [result (usrs/put attr)]
    (is (comparable? attr
                     result)
        "The result contains the correct attributes")
    (is (:id result)
        "The result contains an id value")))

#_(dbtest email-is-required
  (let [result (usrs/put (dissoc attr :email))]
    (is (invalid? result [:email] "Email is required"))
    (is (not (:id result))
        "The result does not contain an :id value")))
#_(dbtest email-is-unique)

#_(dbtest given-name-is-required
  (let [result (usrs/put (dissoc attr :given-name))]
    (is (invalid? result [:given-name] "Given name is required"))
    (is (not (:id result))
        "The result does not contain an :id value")))

#_(dbtest surname-is-required
  (let [result (usrs/put (dissoc attr :surname))]
    (is (invalid? result [:surname] "Surname is required"))
    (is (not (:id result))
        "The result does not contain an :id value")))

(def ^:private update-context
  {:users [#:user{:email "john@doe.com"
                  :given-name "John"
                  :surname "Doe"}]})

#_(dbtest email-is-unique
  (with-context update-context
    (let [result (usrs/put attr)]
      (is (invalid? result [:email] "Email is already in use"))
      (is (not (:id result))
          "The result does not contain an :id value"))))

(dbtest update-a-user
  (with-context update-context
    (let [user (find-user "john@doe.com")
          updated (usrs/put (assoc user :user/given-name "Johnnyboy"))]
      (is (comparable? {:user/given-name "Johnnyboy"}
                       updated)
          "The result contains the updated attributes")
      (is (comparable? {:user/given-name "Johnnyboy"}
                       (usrs/find updated))
          "A retrieved model has the updated attributes"))))

(dbtest delete-a-user
  (with-context update-context
    (let [user (find-user "john@doe.com")]
      (usrs/delete user)
      (is (nil? (usrs/find (:id user)))
          "The user cannot be retrieved after delete"))))

(def ^:private oauth-context
  (-> update-context
      (assoc-in [:users 0 :user/identities]
                {:google "abc123"
                 :github "def456"})
      (update-in [:users]
                 conj
                 #:user{:email "jane@doe.com"
                        :given-name "Jane"
                        :surname "Doe"
                        :identities {:google "def456"
                                     :github "abc123"}})))
; NB The above provider/id pairs contain the same provider and id values
; but grouped differently

(dbtest find-a-user-by-oauth-id
        (let [expected #:user{:email "john@doe.com"
                              :given-name "John"
                              :surname "Doe"
                              :identities {:google "abc123"
                                           :github "def456"}}]
          (with-context oauth-context
            (is (comparable? expected
                             (usrs/find-by-oauth [:google "abc123"]))
                "A given ID is used as-is")
            (is (comparable? expected
                             (usrs/find-by-oauth [:google {:id "abc123"}]))
                "An ID is extracted from a given map"))))

(dbtest create-a-user-from-an-oauth-profile
  (testing "known provider"
    (let [user (usrs/from-oauth [:google
                                 {:id "abc123"
                                  :email "john@doe.com"
                                  :given_name "John"
                                  :family_name "Doe"}])
          expected #:user{:email "john@doe.com"
                          :given-name "John"
                          :surname "Doe"
                          :identities {:google "abc123"}}]
      (is (comparable? expected user)
          "A well-formed user map is returned")))
  (testing "unknown provider"
    (let [logs (atom [])
          result (with-redefs [log/log* (fn [_ & args]
                                          (swap! logs conj args)
                                          nil)]
                   (usrs/from-oauth [:unknown-provider
                                     {:id "abc123"}]))]
      (is (nil? result)
          "Nil is returned")
      (let [[l :as ls] @logs]
        (is (= 1 (count ls))
          "One log entry is written")
        (is (= [:error nil "Unrecognized oauth provider :unknown-provider"]
               l)
            "The error details are logged")))))

(dbtest fetch-a-user-from-an-auth-token
  (with-context
    (testing "current token"
      (let [user (find-user "john@doe.com")
            token (usrs/tokenize user)]
        (is (comparable? user (usrs/detokenize token))
            "The original user record is returned")))
    (testing "expired token"
      (let [user (find-user "john@doe.com")
            token (with-clock (mock-clock (t/minus (t/instant)
                                                   (t/hours 7)))
                    (usrs/tokenize user))]
        (is (nil? (usrs/detokenize token))
            "Nil is returned")))))

; TODO: add an identity
; TODO: remove an identity
