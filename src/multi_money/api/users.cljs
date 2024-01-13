(ns multi-money.api.users
  (:require [cljs.pprint :refer [pprint]]
            [dgknght.app-lib.api-3 :refer [path]]
            [multi-money.api :as api]))

(defn me
  [& {:as opts}]
  (api/get (path :me) opts))
