(ns ^:eftest/synchronized triangulum.config-test
  (:require [clojure.test    :refer [are is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string  :as str]
            [triangulum.config :refer [get-config load-config]]))

(defn- load-test-config [f]
  (binding [triangulum.config/*default-file* "config.example.edn"]
    (load-config "test/data/test_config.edn")
    (f)))

(defn- load-config-output [config-file]
  (let [output-name (str "/tmp/test-" (-> config-file (str/split #"\.") (first)) ".txt")
        file        (io/writer output-name :encoding "UTF-8")]
    (binding [*out* file]
      (load-config (str "test/data/" config-file)))
    (.close file)
    (slurp output-name)))

(use-fixtures :each load-test-config)

(deftest ^:unit get-config-test
  (testing "Get config for single value."
    (is (= "testing" (get-config :mode))))

  (testing "Get config for nested value."
    (is (= "testing-password" (get-config :database :password))))

  (testing "Load a different configuration."
    (load-config "test/data/test_new_config.edn")
    (is (= "production" (get-config :mode)))
    (is (= "super-secret-password" (get-config :database :password))))

  (testing "Invalid configurations"
    (are [config-file] (thrown? java.lang.Exception (load-config-output config-file))
      "test_missing_keys_config.edn"
      "test_invalid_spec_config.edn")))
