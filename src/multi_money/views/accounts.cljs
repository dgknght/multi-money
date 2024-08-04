(ns multi-money.views.accounts
  (:require [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [secretary.core :as sct]
            [reagent.core :as r]
            [reagent.ratom :refer [make-reaction]]
            [goog.string :refer [format]]
            [dgknght.app-lib.dom :as dom]
            [dgknght.app-lib.html :as html]
            [dgknght.app-lib.forms :as forms]
            [dgknght.app-lib.forms-validation :as v]
            [dgknght.app-lib.inflection :refer [humanize]]
            [multi-money.views.components :refer [spinner]]
            [multi-money.accounts :refer [annotate]]
            [multi-money.notifications :refer [toast
                                               alert]]
            [multi-money.icons :refer [icon
                                       icon-with-text]]
            [multi-money.state :refer [+busy
                                       -busy
                                       current-entity
                                       current-page]]
            [multi-money.api.commodities :as cdts]
            [multi-money.api.accounts :as acts]))

(defn- load-commodities
  [page-state]
  (+busy)
  (cdts/select :callback -busy
               :on-success #(swap! page-state
                                   assoc
                                   :commodities
                                   (sort-by :commodity/symbol %))))

(defn- confirm?
  [msg-fmt & args]
  (js/confirm (apply format msg-fmt args)))

(defn- load-accounts
  [page-state]
  (+busy)
  (acts/select :callback -busy
               :on-success #(swap! page-state assoc :accounts (annotate %))))

(defn- delete-account
  [account page-state]
  (when (confirm? "Are you sure you want to delete the account %s?"
                  (:account/name account))
    (acts/delete account :on-success #(load-accounts page-state))))

(defn- account-row
  [account page-state]
  ^{:key (str "account-row-" (:id account))}
  [:tr
   [:td [:div
         {:class (str "left-depth-" (:depth account))}
         (:account/name account)]]
   [:td.text-end [:div.btn-group
                  [:button.btn.btn-sm.btn-secondary
                   {:on-click (fn [_]
                                (swap! page-state assoc :selected account)
                                (dom/set-focus "name"))}
                   (icon :pencil :size :small)]
                  [:button.btn.btn-sm.btn-danger
                   {:on-click #(delete-account account page-state)}
                   (icon :trash :size :small)]]]])

(defn- accounts-table
  [page-state]
  (let [accounts (r/cursor page-state [:accounts])]
    (fn []
      [:table#accounts-table.table
       [:thead
        [:tr
         [:th "Name"]
         [:th (html/space)]]]
       [:tbody
        (cond
          (nil? @accounts)
          [:tr [:td.text-center {:col-span 2} (spinner)]]

          (seq @accounts)
          (->> @accounts
               (map #(account-row % page-state))
               doall)

          :else
          [:tr
           [:td.text-center.text-secondary
            {:col-span 2}
            "Click \"Add\" to create the first account."]])]
       [:tfoot
        [:tr
         [:td.border-bottom-0.pt-3 {:col-span 2}
          [:button.btn.btn-primary
           {:on-click (fn [_]
                        (swap! page-state assoc
                               :selected {:account/commodity (:entity/default-commodity @current-entity)
                                          :account/type "asset"})
                        (dom/set-focus "name"))}
           (icon-with-text :plus "Add")]]]]])))

(defn- save-account
  [page-state]
  (+busy)
  (acts/put (get-in @page-state [:selected])
            :callback -busy
            :on-failure (fn [e]
                          (alert "Unable to save the account.")
                          ; TODO: How to know if this is really a validation error?
                          (pprint {:on-failure e})
                          (swap! page-state assoc :validation-errors (-> e :data :errors)))
            :on-success (fn [_]
                          (load-accounts page-state)
                          (swap! page-state dissoc :selected :validation-errors)
                          (toast "The account was saved successfully"
                                 :header "Save Completed"))))

(defn- account-form
  [page-state]
  (let [accounts (r/cursor page-state [:accounts])
        account (r/cursor page-state [:selected])
        commodities (r/cursor page-state [:commodities])
        local-validation-errors (r/cursor account [::v/validation ::v/messages [:account/name]])
        server-validation-errors (r/cursor page-state [:validation-errors :account/name])
        name-errors (make-reaction #(concat (vals @local-validation-errors)
                                            @server-validation-errors))]
    (fn []
      [:form {:no-validate true
              :on-submit (fn [e]
                           (.preventDefault e)
                           (v/validate account)
                           (when (v/valid? account)
                             (save-account page-state)))}
       [forms/text-field account [:account/name] {:errors name-errors
                                                  :validations #{::v/required}}]
       [forms/select-field
        account
        [:account/type]
        (map (juxt identity humanize)
             [:asset :liability :equity :income :expense])]
       [forms/select-field
        account
        [:account/parent :id]
        (cons ["" "None"]
              (->> @accounts
                   (filter #(= (:account/type %)
                               (:account/type @account)))
                   (remove #(= (:id %)
                               (:id @account)))
                   (map (juxt :id (comp #(str/join ":" %)
                                        :path)))))
        {:caption "Parent"}]
       [forms/select-field
        account
        [:account/commodity :id]
        (map (juxt :id :commodity/symbol)
             @commodities)
        {:caption "Commodity"}]
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
        selected (r/cursor page-state [:selected])
        load (dom/debounce (fn []
                             (swap! page-state dissoc :accounts :commodities)
                             (load-accounts page-state)
                             (load-commodities page-state)))]
    (load)
    (add-watch current-entity ::index (fn [_ _ before after]
                                        (when (not= before after)
                                          (load))))
    (fn []
      [:div.container
       [:h1.mt-3 "Accounts"]
       [:div.row
        [:div.col-md-6
         [accounts-table page-state]]
        (when @selected
          [:div.col-md-6
           [account-form page-state]])]])))

(sct/defroute accounts-path "/accounts" []
  (reset! current-page index))
