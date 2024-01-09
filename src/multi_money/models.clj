(ns multi-money.models
  (:require [clojure.spec.alpha :as s]
            [multi-money.util :refer [non-nil?]]))

(s/def ::id non-nil?)
