(ns multi-money.web-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :refer [attr attr= xml1->]]
            [clojure.pprint :refer [pprint]]
            [ring.mock.request :as req]
            [dgknght.app-lib.test :refer [parse-html-body]]
            [dgknght.app-lib.test-assertions]
            [multi-money.handler :refer [app]]))

(deftest fetch-the-main-page
  (let [res (-> (req/request :get "/")
                app
                parse-html-body)
        parsed (-> res :html-body xml-zip)]
    (is (http-success? res))
    (is (xml1-> parsed
                :html
                :body
                :div
                (attr= :id "app"))
        "The body has the JavaScript mount point")
    (is (= "/cljs-out/dev-main.js"
           (xml1-> parsed
                   :html
                   :body
                   :script
                   (attr :src)))
        "The JavaScript is included")))
