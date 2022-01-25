(ns triangulum.cli-test
  (:require [clojure.test    :refer [are deftest testing]]
            [triangulum.cli  :refer [get-cli-options]]))

(def ^:private cli-actions {:test {:description "Starts the test."}})

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
    (testing "Config uses default values."
      (let [config   {}
            f        #(get-cli-options (concat ["test"] %) cli-options cli-actions "server" config)]
        (are [result args] (let [{:keys [options]} (f args)]
                             (= ((first result) options)
                                (second result)))
             [:int 1]          []
             [:str "test"] []
             [:flag false]      []
             [:set "small"]   [])))

    (testing "CLI flag overrides the default values."
      (let [config   {}
            f        #(get-cli-options (concat ["test"] %) cli-options cli-actions "server" config)]
        (are [result args] (let [{:keys [options]} (f args)]
                             (= ((first result) options)
                                (second result)))
             [:int 3]          ["-i" "3"]
             [:str "override"] ["-s" "override"]
             [:flag true]      ["-f"]
             [:set "medium"]   ["-t" "medium"]))


    (testing "Config overrides the default values."
      (let [config   {:int 3 :str "override" :flag true :set "medium"}
            f        #(get-cli-options (concat ["test"] %) cli-options cli-actions "server" config)]
        (are [result args] (let [{:keys [options]} (f args)]
                             (= ((first result) options)
                                (second result)))
             [:int 3]          []
             [:str "override"] []
             [:flag true]      []
             [:set "medium"]   [])))

    (testing "CLI overrides the config values."
      (let [config   {:int 10 :str "not-valid" :flag false :set "large"}
            f        #(get-cli-options (concat ["test"] %) cli-options cli-actions "server" config)]
        (are [result args] (let [{:keys [options]} (f args)]
                             (= ((first result) options)
                                (second result)))
             [:int 3]          ["-i" "3"]
             [:str "override"] ["-s" "override"]
             [:flag true]      ["-f"]
             [:set "medium"]   ["-t" "medium"])))))
