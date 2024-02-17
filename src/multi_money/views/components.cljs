(ns multi-money.views.components
  (:require [clojure.pprint :refer [pprint]]
            [camel-snake-kebab.core :refer [->kebab-case-string]]
            [goog.string :refer [format]]
            [dgknght.app-lib.inflection :refer [humanize]]
            [multi-money.icons :refer [icon
                                       icon-with-text]]
            [multi-money.state :refer [nav-items
                                       db-strategy]]))

(derive js/String ::string)
(derive cljs.core/PersistentArrayMap ::map)
(derive cljs.core/Keyword ::keyword)

(defmulti ^:private expand-nav-item type)

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
  [{:keys [caption path on-click id active?]}]
  ^{:key (str "dropdown-item-" id)}
  [:li
   [:a.dropdown-item
    (cond-> {:href path
             :on-click on-click}
      active? (assoc :class "active"))
    caption]])

(defn- dropdown
  [items]
  [:ul.dropdown-menu
   (->> items
        (map (comp dropdown-item
                   expand-nav-item))
        doall)])

(defn- nav-item
  [{:keys [path on-click caption children id active?]}]
  ^{:key (str "nav-item-" id)}
  [:li.nav-item {:class (when (seq children) "dropdown")}
   [:a.nav-link.d-flex.align-items-center
    (merge {:href path
            :on-click on-click}
           (when active?
             {:class "active"})
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

(defn- db-strategy-nav-item []
  (let [current @db-strategy]
    {:caption (icon-with-text :database current)
     :active? true
     :children (mapv (fn [s]
                       {:id (format "%s-db-strategy" (name s))
                        :active? (= s current)
                        :caption s
                        :on-click #(reset! db-strategy s)})
                     [:sql :mongo])}))

(defn- build-nav-items []
  [(db-strategy-nav-item)])

(defn title-bar []
  (add-watch db-strategy
             ::title-bar
             (fn [& _]
               (reset! nav-items (build-nav-items))))
  (when-not @nav-items
    (reset! nav-items (build-nav-items)))
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
