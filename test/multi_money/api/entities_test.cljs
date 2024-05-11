(ns multi-money.api.entities-test
  (:require [clojure.test :refer [deftest async is]]
            [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.test-assertions]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.api.entities :as ents]))

(deftest fetch-entities
  (async
    done
    (let [calls (atom [])]
      (with-redefs [api/get (fn [& [_url {:keys [on-success]} :as args]]
                              (swap! calls conj args)
                              (on-success [#:entity{:name "Business"}
                                           #:entity{:name "Business"}]))]
        (ents/select :on-success (fn [entities]
                                   (is (:dgknght.app-lib.test-assertions/seq-of-maps-like?
                                         [#:entity{:name "Business"}
                                          #:entity{:name "Personal"}]
                                         entities)
                                       "The entities are returned")
                                   (is (= 1 (count @calls))
                                       "The entities index API endpoint is called once")
                                   (is (= "/api/entities"
                                          (ffirst @calls))
                                       "The entities index API endpoint is called")
                                   (done)))))))
