(ns multi-money.icons-test
  (:require [clojure.test :refer [deftest is]]
            [multi-money.icons :as icns]))

(deftest create-an-icon-element
  (is (= [:svg.bi
          {:fill "currentColor"
           :width 24
           :height 24}
          [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
         (icns/icon :pencil))))

(deftest create-an-icon-element-with-text
  (is (= [:div.d-flex.align-items-center
          [:svg.bi
           {:fill "currentColor"
            :width 24
            :height 24}
           [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
          [:span.ms-2 "Edit"]]
         (icns/icon-with-text :pencil "Edit"))))
