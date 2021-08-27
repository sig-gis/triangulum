(ns triangulum.config-test
  (:require [clojure.test :refer [are is deftest testing use-fixtures]]
            [triangulum.config :refer [get-config load-config]]))

(defn- load-test-config [f]
  (load-config "test/test_config.edn")
  (f))

(use-fixtures :once load-test-config)

(deftest get-config-test
  (testing "Get config for single value."
    (is (= (get-config :testing)
           1234)))
  (testing "Get config for nested value."
    (is (= (get-config :database :host)
           "localhost"))))

(deftest load-config-test
  (testing "Load a different configuration."
    (load-config "test/test_new_config.edn")
    (is (= (get-config :testing)
           4321))
    (is (= (get-config :database :host)
           "production"))))
