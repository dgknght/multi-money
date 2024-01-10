(ns multi-money.api.users
  (:require [dgknght.app-lib.api-3 :refer [path]]
            [multi-money.api :as api]))

(defn me
  [& {:as opts}]
  (api/get (path :api :users :me) opts))
