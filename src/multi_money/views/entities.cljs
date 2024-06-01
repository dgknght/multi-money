(ns multi-money.views.entities
  (:require [cljs.pprint :refer [pprint]]
            [secretary.core :as sct]
            [reagent.core :as r]
            [goog.string :refer [format]]
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

(defn- confirm?
  [msg-fmt & args]
  (js/confirm (apply format msg-fmt args)))

(defn- load-entities
  [& [entity]]
  (+busy)
  (ents/select :callback -busy
               :on-success (fn [entities]
                             (swap! app-state
                                    #(cond-> (assoc % :current-entities entities)
                                       (= (:id entity)
                                          (:id (:current-entity %)))
                                       (assoc :current-entity entity))))))

(defn- delete-entity
  [entity]
  (when (confirm? "Are you sure you want to delete the entity %s?"
                  (:entity/name entity))
    (ents/delete entity :on-success #(load-entities))))

(defn- entity-row
  [entity page-state]
  (let [selected? (= @current-entity entity)]
    ^{:key (str "entity-row-" (:id entity))}
    [:tr
     [:td (:entity/name entity)]
     [:td.text-end [:div.btn-group
                    [:button.btn.btn-sm
                     {:class (if selected?
                               "btn-success"
                               "btn-outline-success")
                      :on-click (fn [_]
                                  (reset! current-entity entity))}
                     (icon (if selected?
                             :check-circle
                             :circle)
                           :size :small)]
                    [:button.btn.btn-sm.btn-secondary
                     {:on-click (fn [_]
                                  (swap! page-state assoc :selected entity)
                                  (dom/set-focus "name"))}
                     (icon :pencil :size :small)]
                    [:button.btn.btn-sm.btn-danger
                     {:on-click #(delete-entity entity)}
                     (icon :trash :size :small)]]]]))

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
       [:td.border-bottom-0.pt-3 {:col-span 2}
        [:button.btn.btn-primary
         {:on-click (fn [_]
                      (swap! page-state assoc :selected {})
                      (dom/set-focus "name"))}
         (icon-with-text :plus "Add")]]]]]))

(defn- save-entity
  [page-state]
  (+busy)
  (ents/put (get-in @page-state [:selected])
            :callback -busy
            :on-failure (fn [e]
                          ; TODO: How to know if this is really a validation error?
                          (pprint {:on-failure e})
                          (swap! page-state assoc :validation-errors (-> e :data :errors)))
            :on-success (fn [e]
                          (load-entities e)
                          (swap! page-state dissoc :selected :validation-errors)
                          (toast "The entity was saved successfully"
                                 :header "Save Completed"))))

(defn- entity-form
  [page-state]
  (let [selected (r/cursor page-state [:selected])
        name-errors (r/cursor page-state [:validation-errors :entity/name])]
    (fn []
      [:form {:no-validate true
              :on-submit (fn [e]
                           (.preventDefault e)
                           (save-entity page-state))}
       [forms/text-field selected [:entity/name] {:errors name-errors}]
       [:div
        [:button.btn.btn-primary {:type :submit}
         (icon-with-text :floppy2 "Save" :size :small)]
        [:button.btn.btn-secondary.ms-2 {:type :button
                                         :on-click #(swap! page-state
                                                           dissoc :selected
                                                           :validation-errors)}
         (icon-with-text :x-octagon-fill "Cancel" :size :small)]]])))

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
