(ns triangulum.core-test
  (:require [clojure.test :refer [is deftest testing]]
            [triangulum.core :as tri]))

(deftest workflow-test
  (testing "A simple test"
    (is (= 2 (+ 1 1)))))
