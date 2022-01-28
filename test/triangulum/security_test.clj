(ns triangulum.security-test
  (:require [clojure.test        :refer [is deftest testing]]
            [triangulum.security :refer [hash-str hash-file compare-sha256]])
  (:import [java.nio.file Files FileSystem Path]))

(defn- tmp-file! ^Path [dir filename suffix]
  (Files/createTempFile (FileSystem/getPath dir) filename suffix))

(deftest hash-str-test
  (testing "Performs a SHA-256 hash of a string."
    (is (= (hash-str "clojure is great")
           "8cb3449c569eab427908cbdc57204c128ea7217f74124086e464498a26eb34a1"))
    (is (= (hash-str "8cb3449c569eab427908cbdc57204c128ea7217f74124086e464498a26eb34a1")
           "8878dab7316b9de49c695a01ed00be02e3f9899889beebb466bd493cd4bc004e"))))

(deftest hash-file-test
  (testing "Performs a SHA-256 hash of a file."
    (let [tmp (tmp-file! "/tmp" "test-sha" ".txt")]
      (println tmp))))
