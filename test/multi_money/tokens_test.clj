(ns multi-money.tokens-test
  (:require [clojure.test :refer [deftest is testing]]
            [config.core :as config]
            [buddy.sign.jwt :as jwt]
            [multi-money.tokens :as tkns]))

(deftest encode-a-token
  (testing "configured correctly"
    (is (with-redefs [jwt/sign (fn [_d _s]
                                 "abc123")]
          (= "abc123" (tkns/encode {:my "data"})))
        "The encoded data is returned"))
  (testing "not configured correctly"
    (with-redefs [config/env {}]
      (is (thrown-with-msg?
            java.lang.AssertionError
            #"Missing :app-secret configuration"
            (tkns/encode {:my "data"}))))))

(deftest decode-a-token
  (testing "configured correctly"
    (is (with-redefs [jwt/unsign (fn [_d _s]
                                   {:my "data"})]
          (= {:my "data"} (tkns/decode "abc123")))
        "The original data is returned"))
  (testing "not configured correctly"
    (with-redefs [config/env {}]
      (is (thrown-with-msg?
            java.lang.AssertionError
            #"Missing :app-secret configuration"
            (tkns/decode "abc123"))))))
