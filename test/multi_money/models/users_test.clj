(ns multi-money.models.users-test
  (:require [clojure.test :refer [is use-fixtures testing]]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.validation :as v]
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
            [multi-money.db.datomic.ref]))

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

(def ^:private update-context
  {:users [#:user{:email "john@doe.com"
                  :given-name "John"
                  :surname "Doe"}]})

(dbtest email-is-required
  (is (thrown-with-ex-data?
        "Validation failed"
        {::v/errors {:user/email ["Email is required"]}}
        (usrs/put (dissoc attr :user/email)))))

(dbtest email-is-unique
  (with-context update-context
    (is (thrown-with-ex-data?
          "Validation failed"
          {::v/errors {:user/email ["Email is already in use"]}}
          (usrs/put attr)))))

(dbtest given-name-is-required
  (is (thrown-with-ex-data?
        "Validation failed"
        {::v/errors {:user/given-name ["Given name is required"]}}
        (usrs/put (dissoc attr :user/given-name)))))

(dbtest surname-is-required
  (is (thrown-with-ex-data?
        "Validation failed"
        {::v/errors {:user/surname ["Surname is required"]}}
        (usrs/put (dissoc attr :user/surname)))))

(dbtest update-a-user
  (with-context update-context
    (let [user (find-user "john@doe.com")
          updated (usrs/put (-> user
                                (update-in [:id] str) ; test id coersion
                                (assoc :user/given-name "Johnnyboy")))]
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
