(ns triangulum.core-test
  (:require [clojure.test :refer [is deftest testing]]
            [triangulum.build-db]
            [triangulum.database]
            [triangulum.https]
            [triangulum.logging]
            [triangulum.utils]))

(deftest ^:unit workflow-test
  (testing "A simple test"
    (is (= 2 (+ 1 1)))))
