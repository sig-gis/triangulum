(ns triangulum.server-test
  (:require [clojure.test       :refer [deftest is testing]]
            [ring.adapter.jetty :as jetty]
            [triangulum.handler :as handler]
            [triangulum.logging :as logging]
            [triangulum.notify  :as notify]
            [triangulum.server  :refer [start-server!]]
            [triangulum.utils   :as utils]))

;; Side effects are stubbed so start-server! runs without starting a real server.
(deftest ^:unit suppress-server-header-config-test
  (testing "start-server! passes :send-server-version? false to run-jetty"
    (let [captured (atom nil)]
      (with-redefs [utils/resolve-foreign-symbol (constantly (fn [_] {:status 200}))
                    handler/create-handler-stack (fn [& _] (fn [_] {:status 200}))
                    jetty/run-jetty              (fn [_ config] (reset! captured config) nil) ; nil: leave the server atom clean
                    logging/set-log-path!        (fn [& _] nil)
                    notify/available?            (constantly false)]
        (start-server! {:http-port 8080 :handler 'example/handler :mode "dev"}))
      (is (false? (:send-server-version? @captured))))))
