(ns multi-money.db.mongo.types-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.db.mongo.types :as typs])
  (:import org.bson.types.ObjectId))

(def ^:private id-str "00000020f51bb4362eee2a4d")
(def ^:private id (ObjectId. id-str))

(deftest coerce-an-id
  (is (= id (typs/coerce-id id-str))
      "A string is converted to ObjectId")
  (is (= id (typs/coerce-id id))
      "An ObjectId is returned as is"))
