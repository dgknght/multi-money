(ns multi-money.views.commodities
  (:require [cljs.pprint :refer [pprint]]
            [secretary.core :as sct]
            [reagent.core :as r]
            [goog.string :refer [format]]
            [dgknght.app-lib.dom :as dom]
            [dgknght.app-lib.html :as html]
            [dgknght.app-lib.forms :as forms]
            [multi-money.views.components :refer [spinner]]
            [multi-money.notifications :refer [toast
                                               alert]]
            [multi-money.icons :refer [icon
                                       icon-with-text]]
            [multi-money.state :refer [+busy
                                       -busy
                                       current-page]]
            [multi-money.api.commodities :as cdts]))

(defn- confirm?
  [msg-fmt & args]
  (js/confirm (apply format msg-fmt args)))

(defn- load-commodities
  [page-state]
  (+busy)
  (cdts/select :callback -busy
               :on-success #(swap! page-state assoc :commodities %)))

(defn- delete-commodity
  [commodity page-state]
  (when (confirm? "Are you sure you want to delete the commodity %s?"
                  (:commodity/name commodity))
    (cdts/delete commodity :on-success #(load-commodities page-state))))

(defn- commodity-row
  [{:commodity/keys [symbol name] :as commodity} page-state]
  ^{:key (str "commodity-row-" (:id commodity))}
  [:tr
   [:td symbol]
   [:td name]
   [:td.text-end [:div.btn-group
                  [:button.btn.btn-sm.btn-secondary
                   {:on-click (fn [_]
                                (swap! page-state assoc :selected commodity)
                                (dom/set-focus "symbol"))}
                   (icon :pencil :size :small)]
                  [:button.btn.btn-sm.btn-danger
                   {:on-click #(delete-commodity commodity page-state)}
                   (icon :trash :size :small)]]]])

(defn- commodities-table
  [page-state]
  (let [commodities (r/cursor page-state [:commodities])]
    (fn []
      [:table#commodities-table.table
       [:thead
        [:tr
         [:th "Symbol"]
         [:th "Name"]
         [:th (html/space)]]]
       [:tbody
        (cond
          (nil? @commodities)
          [:tr [:td.text-center {:col-span 3} (spinner)]]

          (seq @commodities)
          (->> @commodities
               (map #(commodity-row % page-state))
               doall)
          
          :else
          [:tr [:td.text-center.text-secondary {:col-span 3} "Click \"Add\" to create the first commodity."]])]
       [:tfoot
        [:tr
         [:td.border-bottom-0.pt-3 {:col-span 3}
          [:button.btn.btn-primary
           {:on-click (fn [_]
                        (swap! page-state assoc :selected {:commodity/type "mutual-fund"})
                        (dom/set-focus "symbol"))}
           (icon-with-text :plus "Add")]]]]])))

(defn- save-commodity
  [page-state]
  (+busy)
  (cdts/put (get-in @page-state [:selected])
            :callback -busy
            :on-failure #(alert "Unable to save the commodity.")
            :on-success (fn [{:commodity/keys [name]}]
                          (load-commodities page-state)
                          (swap! page-state dissoc :selected :validation-errors)
                          (toast (format "The commodity \"%s\" was saved successfully"
                                         name)
                                 :header "Save Completed"))))

(defn- commodity-form
  [page-state]
  (let [selected (r/cursor page-state [:selected])
        name-errors (r/cursor page-state [:validation-errors :commodity/name])
        symbol-errors (r/cursor page-state [:validation-errors :commodity/symbol])]
    (fn []
      [:form {:no-validate true
              :on-submit (fn [e]
                           (.preventDefault e)
                           (save-commodity page-state))}
       [forms/text-field selected [:commodity/symbol] {:errors symbol-errors}]
       [forms/text-field selected [:commodity/name] {:errors name-errors}]
       [forms/select-field selected [:commodity/type] [["currency" "Currency"]
                                                       ["mutual-fund" "Mutual Fund"]
                                                       ["stock" "Stock"]]]
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
    (load-commodities page-state)
    (fn []
      [:div.container
       [:h1.mt-3 "Commodities"]
       [:div.row
        [:div.col-md-6
         [commodities-table page-state]]
        (when @selected
          [:div.col-md-6
           [commodity-form page-state]])]])))

(sct/defroute commodities-path "/commodities" []
  (reset! current-page index))
