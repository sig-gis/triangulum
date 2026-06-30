(ns triangulum.database-test
  (:require [clojure.test        :refer [is deftest testing]]
            [next.jdbc           :as jdbc]
            [triangulum.config   :as config]
            [triangulum.database :refer [call-sql coerce-sql-arg]]))

;; coerce-sql-arg fixes the old "narrow every Long to int" path, which overflowed on bigint
;; params like epoch-millisecond timestamps.

(deftest ^:unit coerce-sql-arg-test
  (testing "int-range longs are narrowed to Integer (SQL `integer` params)"
    (is (instance? Integer (coerce-sql-arg 5)))
    (is (= (int 5) (coerce-sql-arg 5)))
    (is (instance? Integer (coerce-sql-arg -5)))
    (is (instance? Integer (coerce-sql-arg (long Integer/MAX_VALUE))))
    (is (instance? Integer (coerce-sql-arg (long Integer/MIN_VALUE)))))

  (testing "out-of-range longs stay Long so bigint params don't overflow"
    (is (instance? Long (coerce-sql-arg 1781208765120)))   ; the value that first hit the overflow
    (is (= 1781208765120 (coerce-sql-arg 1781208765120)))
    (is (instance? Long (coerce-sql-arg (inc (long Integer/MAX_VALUE)))))
    (is (instance? Long (coerce-sql-arg (dec (long Integer/MIN_VALUE))))))

  (testing "in-range doubles become floats; out-of-range doubles stay double"
    (is (instance? Float  (coerce-sql-arg 3.14)))
    (is (instance? Double (coerce-sql-arg 1.0E40)))   ; > Float/MAX_VALUE; narrowing would throw
    (is (= 1.0E40 (coerce-sql-arg 1.0E40)))
    (is (= "abc" (coerce-sql-arg "abc")))
    (is (= :kw (coerce-sql-arg :kw)))
    (is (nil? (coerce-sql-arg nil)))))

(deftest ^:unit coerce-sql-arg-passthrough-test
  (testing "numeric types other than Long/Double are left untouched"
    (is (instance? Integer (coerce-sql-arg (int 7))))
    (is (instance? Float   (coerce-sql-arg (float 1.5))))
    (is (= 5N             (coerce-sql-arg 5N)))
    (is (= 1.5M           (coerce-sql-arg 1.5M)))
    (is (= (biginteger 9) (coerce-sql-arg (biginteger 9))))))

;; call-sql itself must pass an out-of-range arg through as Long (jdbc stubbed, no DB).
(deftest ^:unit call-sql-coerces-args-test
  (testing "call-sql narrows int-range args but keeps bigint args as Long"
    (let [captured (atom nil)]
      (with-redefs [config/get-config   (fn [& _] {})   ; so pg-db needs no config.edn
                    jdbc/get-datasource identity
                    jdbc/execute!       (fn [_ params _] (reset! captured params) [])]
        (call-sql "some_fn" {:log? false} 42 1781208765120))
      (let [[_query small big] @captured]
        (is (instance? Integer small))
        (is (instance? Long big))
        (is (= 1781208765120 big))))))
