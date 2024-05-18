(ns multi-money.api
  (:refer-clojure :exclude [get])
  (:require [cljs.pprint :refer [pprint]]
            [goog.string :refer [format]]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.state :as state]))

(defn- on-failure
  [e]
  (.error js/console "The API call was not successful")
  (.error js/console (:error e))
  (.dir js/console (clj->js (:data e))))

(defn- apply-defaults
  [opts]
  (let [{:keys [db-strategy auth-token]} @state/app-state]
    (cond-> (-> opts
                (update-in [:on-failure] (fnil identity on-failure)))
      db-strategy (assoc-in [:headers "db-strategy"] (name db-strategy))
      auth-token  (assoc-in [:headers "Authorization"] (format "Bearer %s" auth-token)))))

(defn get
  [url opts]
  (api/get url (apply-defaults opts)))

(defn post
  [url resource opts]
  (api/post url resource (apply-defaults opts)))

(defn patch
  [url resource opts]
  (api/patch url resource (apply-defaults opts)))

(defn delete
  [url opts]
  (api/delete url (apply-defaults opts)))
