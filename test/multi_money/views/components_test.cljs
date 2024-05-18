(ns multi-money.views.components-test
  (:require [cljs.test :refer [deftest is]]
            [multi-money.views.components :as c]))

(deftest create-a-navbar
  (is (= [:ul.navbar-nav.me-auto.mb-2.mb-lg-0
          '([:li.nav-item
             {:class nil}
             [:a.nav-link.d-flex.align-items-center
              {:href "fully-implied"
               :on-click nil}
              "Fully implied"]
             nil]
            [:li.nav-item
             {:class nil}
             [:a.nav-link.d-flex.align-items-center
              {:href "implied-caption"
               :on-click nil}
              "Implied caption"]
             nil]
            [:li.nav-item
             {:class nil}
             [:a.nav-link.d-flex.align-items-center
              {:href "explicit-path"
               :on-click nil}
              "Explicit caption"]
             nil]
            [:li.nav-item
             {:class "dropdown"}
             [:a.nav-link.d-flex.align-items-center
              {:href "#"
               :on-click nil
               :class "dropdown-toggle"
               :role :button
               :data-bs-toggle :dropdown
               :aria-expanded false}
              "List header"]
             [:ul.dropdown-menu
              ([:li
                [:a.dropdown-item
                 {:href "child-item-1"
                  :on-click nil}
                 "Child item 1"]]
               [:li [:hr.dropdown-divider]]
               [:li
                [:a.dropdown-item
                 {:href "#"
                  :on-click :fn}
                 "Child item 2"]])]])]
         (c/navbar ["fully-implied"
                    {:path "implied-caption"}
                    {:path "explicit-path"
                     :caption "Explicit caption"}
                    {:caption "List header"
                     :children ["child-item-1"
                                [:divider 1]
                                {:on-click :fn
                                 :caption "Child item 2"}]}]))))
