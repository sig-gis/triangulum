(ns triangulum.core-test
  (:require [clojure.test :refer [is deftest testing]]
            [triangulum.core :as tri]))

(deftest workflow-test
  (testing "A simple test"
    (is (= 2 (+ 1 1)))))

(defn foo
  "Adds two numbers"
  [x y]
  (+ x y))

(deftest foo-test
  (testing "A failing test"
    (is (= 2 (foo)))))
