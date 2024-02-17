(ns multi-money.views.components
  (:require [clojure.pprint :refer [pprint]]
            [camel-snake-kebab.core :refer [->kebab-case-string]]
            [dgknght.app-lib.inflection :refer [humanize]]
            [multi-money.icons :refer [icon]]
            [multi-money.state :refer [nav-items]]))

(derive js/String ::string)
(derive cljs.core/PersistentArrayMap ::map)
(derive cljs.core/Keyword ::keyword)

(defmulti ^:private expand-nav-item type)

(defmethod expand-nav-item :default
  [x]
  (pprint {::no-expend-nav-item-for (type x)}))

(defmethod expand-nav-item ::keyword [k] k)

(defmethod expand-nav-item ::string
  [path]
  (expand-nav-item {:path path}))

(defmethod expand-nav-item ::map
  [{:keys [path id] :as item :or {path "#"}}]
  {:pre [(or (:path item)
             (:caption item))]}
  (-> item
      (assoc :path path
             :id (or id (->kebab-case-string path)))
      (update-in [:caption] (fnil identity (humanize path)))))

(defmulti ^:private dropdown-item type)

(defmethod dropdown-item ::keyword
  [k]
  (case k
    :divider [:li [:hr.dropdown-divider]]))

(defmethod dropdown-item ::map
  [{:keys [caption path on-click id]}]
  ^{:key (str "dropdown-item-" id)}
  [:li
   [:a.dropdown-item
    {:href path
     :on-click on-click}
    caption]])

(defn- dropdown
  [items]
  [:ul.dropdown-menu
   (->> items
        (map (comp dropdown-item
                   expand-nav-item))
        doall)])

(defn- nav-item
  [{:keys [path on-click caption children id]}]
  ^{:key (str "nav-item-" id)}
  [:li.nav-item {:class (when (seq children) "dropdown")}
   [:a.nav-link (merge {:href path
                        :on-click on-click}
                       (when (seq children)
                         {:class "dropdown-toggle"
                          :role :button
                          :data-bs-toggle :dropdown
                          :aria-expanded false}))
    caption]
   (when (seq children)
     (dropdown children))])

(defn navbar [nav-items]
  [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
   (->> nav-items
        (map (comp nav-item
                   expand-nav-item))
        doall)])

(def ^:private unauthenticated-nav-items
  [{:caption "DB Strategy"
    :children [{:id "sql-db-strategy"
                :caption "SQL"
                :on-click #(println "change to sql")}
               {:id "mongo-db-strategy"
                :caption "MongoDB"
                :on-click #(println "change to mongo")}]}])

(defn title-bar []
  (when-not @nav-items
    (reset! nav-items unauthenticated-nav-items))
  (fn []
    [:nav.navbar.navbar-expand-lg.bg-body-tertiary.rounded.mt-1
     {:aria-label "Primary Navigation Menu"}
     [:div.container-fluid
      [:a.navbar-brand {:href "/"}
       (icon :cash-stack :size :large)]
      [:button.navbar-toggler {:type :button
                               :data-bs-toggle :collapse
                               :data-bs-target "#primary-nav"
                               :aria-controls "primary-nav"
                               :aria-expanded false
                               :aria-label "Toggle Navigation"}
       [:span.navbar-toggler-icon]]
      [:div#primary-nav.collapse.navbar-collapse
       [navbar @nav-items]]]]))
