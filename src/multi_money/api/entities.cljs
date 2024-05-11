(ns multi-money.api.entities
  (:refer-clojure :exclude [select])
  (:require [dgknght.app-lib.api-3 :as api]))

(defn select
  [& opts]
  (api/get (api/path :entities) opts))
