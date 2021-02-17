(ns triangulum.core-test
  (:use [clojure.test])
  (:require [triangulum.core :as tri]))

(deftest workflow-test
  (testing "A simple test"
    (is (= 2 (+ 1 1)))))
