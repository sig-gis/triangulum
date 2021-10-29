(ns ^:eftest/synchronized triangulum.config-test
  (:require [clojure.test :refer [is deftest testing use-fixtures]]
            [triangulum.config :refer [get-config load-config] :as config]))

(defn- load-test-config [f]
  (binding [config/*default-file* "config.example.edn"]
    (load-config "test/test_config.edn")
    (f)))

(use-fixtures :each load-test-config)

(deftest get-config-test
  (testing "Get config for single value."
    (is (= "testing" (get-config :mode))))

  (testing "Get config for nested value."
    (is (= "testing-password" (get-config :database :password))))

  (testing "Load a different configuration."
    (load-config "test/test_new_config.edn")
    (is (= "production" (get-config :mode)))
    (is (= "super-secret-password" (get-config :database :password)))))
