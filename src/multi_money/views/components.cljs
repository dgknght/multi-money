(ns multi-money.views.components
  (:require [cljs.pprint :refer [pprint]]
            [camel-snake-kebab.core :refer [->kebab-case-string]]
            [goog.string :refer [format]]
            [reagent.ratom :refer [make-reaction]]
            [dgknght.app-lib.inflection :refer [humanize]]
            [multi-money.config :refer [env]]
            [multi-money.icons :refer [icon
                                       icon-with-text]]
            [multi-money.state :refer [nav-items
                                       sign-out
                                       current-user
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

(defn- authenticated-nav-items []
  [{:path "/commodities"
    :caption "Commodities"}
   {:caption "Sign Out"
    :on-click sign-out}])

(defn- build-nav-items []
  (if @current-user
    (authenticated-nav-items)
    []))

(defn title-bar []
  (doseq [x [db-strategy current-user current-entities current-entity]]
    (add-watch x
               ::title-bar
               (fn [& _]
                 (reset! nav-items (build-nav-items)))))

  (when-not @nav-items
    (reset! nav-items (build-nav-items)))

  (let [brand-caption (make-reaction #(or (:entity/name @current-entity)
                                       (:app-name env)
                                       "Multi-Money"))
        brand-attr (make-reaction #(if @current-entity
                                     {:href "#entity-offcanvas"
                                      :role :button
                                      :aria-controls "entity-offcanvas"
                                      :data-bs-toggle :offcanvas}
                                     {:href "/"}))]
    (fn []
      [:nav.navbar.navbar-expand-lg.bg-body-tertiary.rounded.mt-1
       {:aria-label "Primary Navigation Menu"}
       [:div.container-fluid
        [:a.navbar-brand @brand-attr
         (icon :cash-stack :size :large)
         [:span.ms-2 @brand-caption]]
        [:button.navbar-toggler {:type :button
                                 :data-bs-toggle :collapse
                                 :data-bs-target "#primary-nav"
                                 :aria-controls "primary-nav"
                                 :aria-expanded false
                                 :aria-label "Toggle Navigation"}
         [:span.navbar-toggler-icon]]
        [:div#primary-nav.collapse.navbar-collapse
         [navbar @nav-items]]]])))

(defn- click-button
  [elem-id]
  (.click (.getElementById js/document elem-id)))

(defn- hide-entity-offcanvas []
  (click-button "entity-offcanvas-close"))

(defn- entity-list-item
  [entity]
  ^{:key (str "entity-list-item-" (:id entity))}
  [:button.list-group-item.list-group-item-action
   {:class (when (= entity @current-entity) "active")
    :on-click (fn [e]
                (.preventDefault e)
                (reset! current-entity entity)
                (hide-entity-offcanvas))}
   (:entity/name entity)])

(defn- entity-list []
  (fn []
    [:div.row
     [:div.col-md-6.offset-md-3
      [:div.list-group
       (->> @current-entities
            (map entity-list-item)
            doall)]]
     [:div.col-md-1.mt-3.mt-lg-0
      [:a.btn.btn-primary
       {:href "/entities"
        :on-click #(hide-entity-offcanvas)}
       "Manage"]]]))

(defn entity-offcanvas []
  [:div#entity-offcanvas.offcanvas.offcanvas-top
   {:tab-index -1
    :aria-labelledby "entity-offcanvas-title"}
   [:div.offcanvas-header
    [:h5#entity-offcanvas-title.offcanvas-title "Entities"]
    [:button#entity-offcanvas-close.btn-close
     {:type :button
      :data-bs-dismiss :offcanvas
      :aria-label "Close"}]]
   [:div.offcanvas-body
    [entity-list]]])

(defn- hide-db-strategy-offcanvas []
  (click-button "db-strategy-offcanvas-close"))

(def ^:private db-strategies
  {"sql" "SQL"
   "mongo" "MongoDB"
   "datomic-peer" "Datomic Peer"
   "datomic-client" "Datomic Client"})

(defn- db-strategy-list-item
  [[strategy-id caption] & {:keys [current enabled?]}]
  ^{:key (str "db-strategy-list-item-" strategy-id)}
  [:button.list-group-item.list-group-item-action
   {:class (when (= strategy-id current) "active")
    :disabled (not enabled?)
    :on-click (fn [e]
                (.preventDefault e)
                (reset! db-strategy strategy-id)
                (hide-db-strategy-offcanvas))}
   caption])

(defn- db-strategy-list []
  (fn []
    [:div.list-group
     (->> db-strategies
          (map #(db-strategy-list-item %
                                       :current @db-strategy
                                       :enabled? (not @current-user)))
          doall)]))

(defn db-strategy-offcanvas []
  [:div#db-strategy-offcanvas.offcanvas.offcanvas-bottom
   {:tab-index -1
    :aria-labelledby "db-strategy-offcanvas-title"}
   [:div.offcanvas-header
    [:h5#db-strategy-offcanvas-title.offcanvas-title "DB Strategy"]
    [:button#db-strategy-offcanvas-close.btn-close
     {:type :button
      :data-bs-dismiss :offcanvas
      :aria-label "Close"}]]
   [:div.offcanvas-body
    [:div.row
     [:div.col-md-6.offset-md-3
      [db-strategy-list]]
     [:div.col-md-3
      {:class (when-not @current-user "d-none")}
      [:div.alert.alert-info
       {:role :alert}
       "Sign out in order to change the DB strategy"]]]]])

(defn footer []
  (fn []
    [:footer.w-100
     {:style {:position :absolute
              :bottom 0}}
     [:div.container.border-top.mt-3.py-3
      [:a.text-decoration-none.link-body-emphasis
       {:data-bs-toggle :offcanvas
        :href "#db-strategy-offcanvas"
        :role :button
        :aria-controls "db-strategy-offcanvas"}
       (icon-with-text :database (db-strategies @db-strategy))]]]))

(defn spinner []
  [:div.spinner-border {:role :status}
   [:span.visually-hidden "Loading..."]])
