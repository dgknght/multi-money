(ns multi-money.views.entities
  (:require [cljs.pprint :refer [pprint]]
            [secretary.core :as sct]
            [reagent.core :as r]
            [dgknght.app-lib.dom :as dom]
            [dgknght.app-lib.html :as html]
            [dgknght.app-lib.forms :as forms]
            [multi-money.notifications :refer [toast]]
            [multi-money.icons :refer [icon
                                       icon-with-text]]
            [multi-money.state :refer [+busy
                                       -busy
                                       app-state
                                       current-page
                                       current-entities
                                       current-entity]]
            [multi-money.api.entities :as ents]))

(defn- entity-row
  [entity page-state]
  (let [css (when (= @current-entity entity) "bg-success-subtle text-success-emphasis")]
    ^{:key (str "entity-row-" (:id entity))}
    [:tr
     [:td {:class css} (:entity/name entity)]
     [:td.text-end {:class css} [:div.btn-group
                    [:button.btn.btn-sm.btn-secondary
                     {:on-click (fn [_]
                                  (swap! page-state assoc :selected entity)
                                  (dom/set-focus "name"))}
                     (icon :pencil :size :small)]]]]))

(defn- entities-table
  [page-state]
  (fn []
    [:table#entities-table.table
     [:thead
      [:tr
       [:th "Name"]
       [:th (html/space)]]]
     [:tbody
      (->> @current-entities
           (map #(entity-row % page-state))
           doall)]
     [:tfoot
      [:tr
       [:td {:col-span 2}
        [:button.btn.btn-primary
         {:on-click (fn [_]
                      (swap! page-state assoc :selected {})
                      (dom/set-focus "name"))}
         (icon-with-text :plus "Add")]]]]]))

(defn- load-entities
  [entity]
  (+busy)
  (ents/select :callback -busy
               :on-success (fn [entities]
                             (swap! app-state
                                    #(cond-> (assoc % :current-entities entities)
                                       (= (:id entity)
                                          (:id (:current-entity %)))
                                       (assoc :current-entity entity))))))

(defn- save-entity
  [page-state]
  (+busy)
  (ents/put (get-in @page-state [:selected])
            :callback -busy
            :on-success (fn [e]
                          (load-entities e)
                          (swap! page-state dissoc :selected)
                          (toast "The entity was saved successfully"
                                 :header "Save Completed"))))

(defn- entity-form
  [page-state]
  (let [selected (r/cursor page-state [:selected])]
    (fn []
      [:form {:no-validate true
              :on-submit (fn [e]
                           (.preventDefault e)
                           (save-entity page-state))}
       [forms/text-field selected [:entity/name]]
       [:div
        [:button.btn.btn-primary {:type :submit}
         "Save"]
        [:button.btn.btn-secondary.ms-2 {:type :button
                                         :on-click #(swap! page-state dissoc :selected)}
         "Cancel"]]])))

(defn- index []
  (let [page-state (r/atom {})
        selected (r/cursor page-state [:selected])]
    (fn []
      [:div.container
       [:h1.mt-3 "Entities"]
       [:div.row
        [:div.col-md-6
         [entities-table page-state]]
        (when @selected
          [:div.col-md-6
           [entity-form page-state]])]])))

(sct/defroute entities-path "/entities" []
  (reset! current-page index))
