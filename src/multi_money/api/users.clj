(ns multi-money.api.users
  (:require [clojure.pprint :refer [pprint]]
            [dgknght.app-lib.api :as api]))

(defn- me
  [{:keys [authenticated]}]
  (api/response authenticated))

(def routes
  ["/me" {:get {:handler me}}])
