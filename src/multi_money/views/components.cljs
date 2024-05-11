(ns multi-money.views.components
  (:require [cljs.pprint :refer [pprint]]
            [camel-snake-kebab.core :refer [->kebab-case-string]]
            [goog.string :refer [format]]
            [dgknght.app-lib.inflection :refer [humanize]]
            [multi-money.icons :refer [icon
                                       icon-with-text]]
            [multi-money.state :refer [nav-items
                                       current-entity
                                       current-entities
                                       db-strategy]]))

(derive js/String ::string)
(derive cljs.core/PersistentArrayMap ::map)
(derive cljs.core/Keyword ::keyword)
(derive cljs.core/PersistentVector ::vector)

(defmulti ^:private expand-nav-item type)

(defmethod expand-nav-item :default
  [x]
  (.error js/console "Unhandled nav item type")
  (pprint x))

(defmethod expand-nav-item ::vector [k] k)

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

(defmethod dropdown-item ::vector
  [[item-type id]]
  (case item-type
    :divider (with-meta
               [:li [:hr.dropdown-divider]]
               {:key (format "dropdown-divider-%s" id)})))

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
  [{:keys [path on-click caption children id active?] :as item}]
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

(def ^:private db-strategies
  {"sql" "SQL"
   "mongo" "MongoDB"
   "datomic-peer" "Datomic Peer"
   "datomic-client" "Datomic Client"})

(defn- db-strategy-nav-item []
  (let [current @db-strategy]
    {:id :db-strategy-menu
     :caption (icon-with-text :database (db-strategies current))
     :children (mapv (fn [[id caption]]
                       {:id (format "%s-db-strategy" id)
                        :active? (= id current)
                        :caption caption
                        :on-click #(reset! db-strategy id)})
                     db-strategies)}))

(defn- entity-nav-item
  [{:keys [id] :entity/keys [name] :as entity}]
  {:id (format "entity-menu-option-%s" id)
   :caption name
   :on-click #(reset! current-entity entity)})

(defn- entities-nav-item []
  {:id :entities-menu
   :caption (or (:name @current-entity) "Entities")
   :children (->> [(when (seq @current-entities) [:divider 1])
                   {:path "/entities"
                    :caption "Manage Entities"}]
                  (remove nil?)
                  (concat (map entity-nav-item @current-entities))
                  (into []))})

(defn- build-nav-items []
  [(db-strategy-nav-item)
   (entities-nav-item)])

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

(defn footer []
  (fn []
    [:footer.w-100
     {:style {:position :absolute
              :bottom 0}}
     [:div.container.border-top.mt-3.py-3 @db-strategy]]))
