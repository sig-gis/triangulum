(ns triangulum.security-test
  (:require [clojure.java.io     :as io]
            [clojure.test        :refer [is deftest testing use-fixtures]]
            [triangulum.security :refer [hash-digest hash-file]]
            [triangulum.utils    :refer [delete-recursively]]))

(defn- cleanup-tmp [f]
  (f)
  (when (.exists (io/file "tmp"))
    (delete-recursively "tmp")))

(use-fixtures :each cleanup-tmp)

(deftest ^:unit hash-digest-test
  (testing "Performs a SHA-256 hash of a string."
    (is (= (hash-digest "clojure is great")
           "8cb3449c569eab427908cbdc57204c128ea7217f74124086e464498a26eb34a1"))
    (is (= (hash-digest "8cb3449c569eab427908cbdc57204c128ea7217f74124086e464498a26eb34a1")
           "8878dab7316b9de49c695a01ed00be02e3f9899889beebb466bd493cd4bc004e"))))

(deftest ^:unit hash-file-test
  (testing "Performs a SHA-256 hash of a file."
    (let [tmp-file "tmp/test-sha.txt"]

      (io/make-parents tmp-file)
      (spit tmp-file "clojure is great")

      (is (= (hash-file tmp-file)
             "8cb3449c569eab427908cbdc57204c128ea7217f74124086e464498a26eb34a1")))))
