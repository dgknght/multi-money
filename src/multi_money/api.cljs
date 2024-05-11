(ns multi-money.api
  (:refer-clojure :exclude [get])
  (:require [cljs.pprint :refer [pprint]]
            [goog.string :refer [format]]
            [dgknght.app-lib.api-3 :as api]
            [multi-money.state :as state]))

(defn handle-error
  [error]
  (.error js/console "Unexpected error during API call.")
  (when-let [d (ex-data error)]
    (pprint {::ex-data d}))
  (.dir js/console error))

(defn- on-failure
  [res]
  (.error js/console "The API call was not successful")
  (.dir js/console res))

(defn- apply-defaults
  [{:as opts
    :keys [on-error]
    :or {on-error handle-error}}]
  (let [{:keys [db-strategy auth-token]} @state/app-state]
    (cond-> (assoc opts
                   :on-error on-error
                   :on-failure on-failure)
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
