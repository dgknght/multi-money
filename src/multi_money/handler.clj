(ns multi-money.handler)

(defn app [_request]
  {:status 200
   :header {"Content-Type" "text/html"}
   :body "<html><body><h1>Multimoney</h1></body></html>"})
