(ns multi-money.config)

(def env
  (js->clj (js/JSON.parse (.-CONFIG js/window)) :keywordize-keys true))
