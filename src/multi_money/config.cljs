(ns multi-money.config)

(def env
  (js->clj (.-CONFIG js/window) :keywordize-keys true))
