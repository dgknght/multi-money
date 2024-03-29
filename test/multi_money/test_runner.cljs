;; This test runner is intended to be run from the command line
(ns multi-money.test-runner
  (:require
    ;; require all the namespaces that you want to test
    [multi-money.util-test]
    [multi-money.icons-test]
    [multi-money.views.components-test]
    [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& _args]
  (run-tests-async 5000))
