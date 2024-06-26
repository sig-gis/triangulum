(ns triangulum.utils-test
  (:require [clojure.test     :refer [is deftest testing]]
            [triangulum.utils :refer [end-with
                                      #_{:clj-kondo/ignore [:deprecated-var]}
                                      data-response
                                      format-str
                                      format-with-dict
                                      kebab->snake
                                      filterm
                                      parse-as-sh-cmd
                                      mapm
                                      find-missing-keys]]))

(deftest ^:unit end-with-test
  (testing "Appends end-value when string doesn't end with end-value."
    (is (= (end-with "path" "/")
           "path/")))

  (testing "Does not append end-value when string does end with end-value."
    (is (= (end-with "path/" "/")
           "path/"))))

(deftest ^:unit format-str-test
  (testing "Allows any char after % to be used in formatting"
    (is (= (format-str "SELECT * FROM %1 WHERE %2 = %3" "table" "column" "'value'")
           "SELECT * FROM table WHERE column = 'value'"))))

(deftest ^:unit kebab->snake-test
  (testing "Converts kebab-str to snake_str"
    (is (= (kebab->snake "my-string") "my_string"))
    (is (= (kebab->snake "MY-STRING") "MY_STRING"))))

(deftest ^:unit parse-as-sh-cmd-test
  (testing "Parses a string into a sh friendly array"
    (is (= (parse-as-sh-cmd "psql -p 5432 -U=username -W")
           ["psql" "-p" "5432" "-U=username" "-W"]))))

#_{:clj-kondo/ignore [:deprecated-var]}
(deftest data-response-test
  (testing "Defaults to status 200, body encoded as edn"
    (let [res (data-response {:message "Hello world"})]
      (is (= (:status res) 200))
      (is (= (-> res (:headers) (get "Content-Type")) "application/edn"))))

  (testing "Body can be encoded as JSON"
    (let [res (data-response {:message "Hello world"} {:type :json})]
      (is (= (:status res) 200))
      (is (= (-> res (:headers) (get "Content-Type")) "application/json"))))

  (testing "Body can be encoded as CLJ Transit"
    (let [res (data-response {:message "Hello world"} {:type :transit})]
      (is (= (:status res) 200))
      (is (= (-> res (:headers) (get "Content-Type")) "application/transit+json"))))

  (testing "Status code can be provided"
    (let [res (data-response {:message "Not Found"} {:status 404})]
      (is (= (:status res) 404)))))

(def ^:private test-map {:a 1 :b 2 :c 3 :d 4})

(deftest mapm-test
  (testing "Applies f to each entry of the map, returns map."
    (is (= (mapm (fn [[k v]] [k (+ v 1)]) test-map)
           {:a 2 :b 3 :c 4 :d 5}))))

(deftest ^:unit filterm-test
  (testing "Filter map using pred, returns a map"
    (is (= (filterm (fn [[_ v]] (odd? v)) test-map)
           {:a 1 :c 3}))))

(deftest ^:unit find-missing-keys-test
  (testing "Two nested maps with same keys return empty set."
    (is (= #{}
           (find-missing-keys {:a "b" :c {:d "e"}} {:a "g" :c {:d "f"}}))))
  (testing "Two nested maps with same keys in different order return empty set."
    (is (= #{}
           (find-missing-keys {:c {:d "e"} :a "b"} {:a "g" :c {:d "f"}}))))
  (testing "Two nested maps, one with more keys return empty set."
    (is (= #{}
           (find-missing-keys {:a "b" :c {:d "e"}} {:a "g" :c {:d "f" :h "i"}}))))
  (testing "Maps without the same keys return invalid keys as set."
    (is (= #{:d :e :f :g :i}
           (find-missing-keys {:a "b" :c {:d "e"} :e {:f "f" :g "g"} :h {:i "i"}}
                              {:a "g" :c {:y "z"} :h nil})))))

(deftest ^:unit format-with-dict-test
  (testing "nil"
    (is (= "hi," (format-with-dict "hi,{{x}}" {:x nil}))))
  (testing "absence"
    (is (= "hi," (format-with-dict "hi,{{x}}" {}))))
  (testing "presence"
    (is (= "hi,false" (format-with-dict "hi,{{x}}" {:x false})))
    (is (= "hi,42" (format-with-dict "hi,{{x}}" {:x 42})))
    (is  (= "hi,nice" (format-with-dict "hi,{{x}}" {:x "nice"})))))
