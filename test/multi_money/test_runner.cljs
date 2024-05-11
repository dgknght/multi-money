(ns multi-money.test-runner
  (:require
    [multi-money.util-test]
    [multi-money.icons-test]
    [multi-money.views.components-test]
    [multi-money.api.entities-test]
    #_[multi-money.api.users-test]
    [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& _args]
  (run-tests-async 5000))
