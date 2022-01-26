(ns triangulum.cli-test
  (:require [clojure.test    :refer [are is deftest testing]]
            [triangulum.cli  :refer [get-cli-options]]))

(def ^:private cli-actions {:run-test {:description "Starts the test."}})

(def ^:private cli-options
  {:int  ["-i" "--int INT" "Integer option, defaults to 1"
          :parse-fn #(if (int? %) % (Integer/parseInt %))
          :default 1]
   :str  ["-s" "--str STING" "String option, defaults to 'test'"
          :default "test"]
   :flag ["-f" "--flag" "Boolean option, defaults to false."
          :default false]
   :set  ["-t" "--set SET" "Set option, can be either \"big\", \"medium\", or \"small\". Defaults to \"small\"."
          :default "small"
          :validate [#{"big" "medium" "small"} "Must be \"big\", \"medium\", or \"small\""]]})

(deftest test-cli-options
  (let [defaults {:int 1 :str "test" :flag false :set "small"}
        sut (fn [args config]
              (get-cli-options (concat ["run-test"] args) cli-options cli-actions "run-test" config))]

    (testing "Config uses default values."
      (is (= (:options (sut [] {})) defaults)))

    (testing "CLI flag overrides the default values."
      (are [args result] (= (:options (sut args {})) (merge defaults result))
           ["-i" "3"]        {:int 3}
           ["-s" "override"] {:str "override"}
           ["-f"]            {:flag true}
           ["-t" "medium"]   {:set "medium"}))

    (testing "Config overrides the default values."
      (let [config {:int 3 :str "override" :flag true :set "medium"}]
        (is (= (:options (sut [] config)) config))))

    (testing "CLI overrides the config values."
      (let [config {:int 10 :str "not-valid" :flag false :set "large"}]
        (are [args result] (= (:options (sut args config)) (merge config result))
             ["-i" "3"]        {:int 3}
             ["-s" "override"] {:str "override"}
             ["-f"]            {:flag true}
             ["-t" "medium"]   {:set "medium"})))))
