(ns multi-money.icons-test
  (:require [clojure.test :refer [deftest testing is]]
            [multi-money.icons :as icns]))

(deftest create-an-icon-element
  (testing "default size"
    (is (= [:svg.bi
            {:fill "currentColor"
             :width 24
             :height 24}
            [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
           (icns/icon :pencil))))
  (testing "default small"
    (is (= [:svg.bi
            {:fill "currentColor"
             :width 16
             :height 16}
            [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
           (icns/icon :pencil :size :small))))
  (testing "default medium"
    (is (= [:svg.bi
            {:fill "currentColor"
             :width 24
             :height 24}
            [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
           (icns/icon :pencil :size :medium))))
  (testing "default medium-large"
    (is (= [:svg.bi
            {:fill "currentColor"
             :width 32
             :height 32}
            [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
           (icns/icon :pencil :size :medium-large))))
  (testing "default large"
    (is (= [:svg.bi
            {:fill "currentColor"
             :width 40
             :height 40}
            [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
           (icns/icon :pencil :size :large)))))

(deftest create-an-icon-element-with-text
  (is (= [:span.d-flex.align-items-center
          [:svg.bi
           {:fill "currentColor"
            :width 24
            :height 24}
           [:use {:href "/images/bootstrap-icons.svg#pencil"}]]
          [:span.ms-2 "Edit"]]
         (icns/icon-with-text :pencil "Edit"))))
