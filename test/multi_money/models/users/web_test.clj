(ns multi-money.models.users.web-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as log]
            [multi-money.tokens :as tkns]
            [multi-money.oauth :as oauth]
            [multi-money.models.users :as usrs]
            [multi-money.models.users.web :as u]))

(deftest lookup-a-user-from-auth-token
  (let [token (tkns/encode {:user-id 1
                            :user-agent "test agent"})
        result (with-redefs [usrs/detokenize (fn [_]
                                               {:id 1
                                                :user/given-name "John"})]
                 (u/validate-token-and-lookup-user
                   {:headers {"authorization" (format "Bearer %s" token)
                              "user-agent" "test agent"}}))]
    (is (= {:id 1
            :user/given-name "John"}
           result))))

(deftest reject-an-invalid-user-agent
  (let [token (tkns/encode {:user-id 1
                            :user-agent "test agent"})
        result (u/validate-token-and-lookup-user
                 {:headers {"authorization" (format "Bearer %s" token)
                            "user-agent" "wrong agent"}})]
    (is (nil? result))))

(deftest fetch-oauth-profile-middleware
  (let [calls (atom [])
        handler (fn [req]
                  (swap! calls conj req)
                  {:body "OK"})
        wrapped (u/wrap-fetch-oauth-profile handler)]
    (with-redefs [oauth/fetch-profiles (fn [& _]
                                         {:google {:first-name "John"}})]
      (wrapped {:oauth2/access-tokens {:google "abc123"}}))
    (let [[c :as cs] @calls]
      (is (= 1 (count cs))
          "The handler is called once")
      (is (= {:google {:first-name "John"}}
             (:oauth2/profiles c))
          "The profiles are assoced to the request"))))

(deftest issue-an-auto-token-middleware
  (let [handler (constantly {:body "OK"})
        wrapped (u/wrap-issue-auth-token handler)
        res (with-redefs [tkns/encode (fn [_]
                                        "abc123")]
              (wrapped {:authenticated {:id 1
                                        :user/given-name "John"}
                        :headers {"user-agent" "test agent"}}))]
    (is (= {:value "abc123"
            :same-site :strict
            :max-age 21600} ; six hours
           (get-in res [:cookies "auth-token"]))
        "The an auth token cookie is added to the response")))

(deftest lookup-or-create-user-middleware
  (testing "user creation"
    (let [calls (atom [])
          handler (fn [req]
                    (swap! calls conj req)
                    {:body "OK"})
          wrapped (u/wrap-user-lookup handler)]
      (with-redefs [usrs/put (fn [u]
                               (assoc u :id 101))
                    usrs/find-by-oauth (constantly nil)]
        (wrapped {:oauth2/profiles {:google {:id "def456"
                                             :given_name "John"
                                             :family_name "Doe"
                                             :email "john@doe.com"}}}))
      (let [[c :as cs] @calls]
        (is (= 1 (count cs))
            "The handler is called once")
        (is (= {:id 101
                :user/identities {:google "def456"}
                :user/given-name "John"
                :user/surname "Doe"
                :user/email "john@doe.com"}
               (:authenticated c))
            "The user is appended to the request"))))
  (testing "exception"
    (let [state (atom {:calls []})
          handler (fn [req]
                    (swap! state update-in [:calls] conj req)
                    {:body "OK"})
          wrapped (u/wrap-user-lookup handler)]
      (with-redefs [usrs/put (fn [_]
                               (throw (RuntimeException. "Induced error")))
                    usrs/find-by-oauth (constantly nil)
                    log/log* (fn [& args]
                                (swap! state update-in [:logs] conj args)
                                nil)]
        (wrapped {:oauth2/profiles {:google {:id "def456"
                                             :given_name "John"
                                             :family_name "Doe"
                                             :email "john@doe.com"}}}))
      (let [{[c :as cs] :calls logs :logs} @state]
        (is (= 1 (count cs))
            "The handler is called once")
        (is (nil? (:authenticated c))
            "The user is appended to the request")
        (is (= 1 (count logs))
            "The error is logged")))))
