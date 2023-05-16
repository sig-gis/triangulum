(ns ^:eftest/synchronized triangulum.config-test
  (:require [clojure.test    :refer [is deftest testing use-fixtures]]
            [clojure.java.io :as io]
            [clojure.string  :as str]
            [triangulum.config :refer [get-config load-config]]))

(defn- load-test-config [f]
  (load-config "test/data/test_config.edn")
  (f))

(defn- load-config-output [config-file]
  (let [output-name (str "/tmp/test-" (-> config-file (str/split #"\.") (first)) ".txt")]
    (with-open [file-writer (io/writer output-name :encoding "UTF-8")]
      (binding [*out* file-writer]
        (load-config (str "test/data/" config-file))))
    (slurp output-name)))

(use-fixtures :each load-test-config)

(deftest ^:unit get-config-test
  (testing "Get config for single value."
    (is (map? (get-config :database))))

  (testing "Get config for nested value."
    (is (= "testing-password" (get-config :database :password))))

  (testing "Load a different configuration."
    (load-config "test/data/test_new_config.edn")
    (is (map? (get-config :database)))
    (is (= "super-secret-password" (get-config :database :password))))

  (testing "Invalid configurations"
    (is (thrown? java.lang.Exception (load-config-output "test_invalid_spec_config.edn")))))
