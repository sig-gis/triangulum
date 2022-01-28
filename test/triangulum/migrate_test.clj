(ns triangulum.migrate-test
  (:require [clojure.test         :refer [is deftest testing use-fixtures run-tests]]
            [clojure.java.io      :as io]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-lower-maps]]
            [triangulum.security  :refer [hash-str]]
            [triangulum.migrate   :refer [migrate!] :as m]))

;; Helpers

;; https://gist.github.com/edw/5128978
(defn delete-recursively
  "Recursively deletes all files in `dir`."
  [dir]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (clojure.java.io/delete-file f))]
    (func func (clojure.java.io/file dir))))

(defn- get-ds [database user user-pass]
  (jdbc/get-datasource {:dbtype                "postgresql"
                        :dbname                database
                        :user                  user
                        :password              user-pass
                        :reWriteBatchedInserts true}))

(defn- get-admin-ds []
  (get-ds "postgres"
          (or (System/getenv "PGUSER") "postgres")
          (or (System/getenv "PGPASSWORD") "")))

;; Fixtures
(defn- set-migrations-dir [f]
  (delete-recursively "tmp")
  (with-bindings {#'m/*migrations-dir* "tmp/sql/changes/"}
    (f)))

(def ^:private create-db ["CREATE ROLE tri_test WITH LOGIN CREATEDB PASSWORD 'tri_test';" "CREATE DATABASE tri_test WITH OWNER tri_test;"])
(def ^:private drop-db   ["DROP DATABASE IF EXISTS tri_test;" "DROP ROLE IF EXISTS tri_test"])

(defn- setup-test-db [f]
  (let [ds (get-admin-ds)]
    (jdbc/execute-one! ds [(first drop-db)])
    (jdbc/execute-one! ds [(second drop-db)])
    (jdbc/execute-one! ds [(first create-db)])
    (jdbc/execute-one! ds [(second create-db)]))

  (f)
  (let [ds (get-admin-ds)]
    (jdbc/execute-one! ds [(first drop-db)])
    (jdbc/execute-one! ds [(second drop-db)])))

(use-fixtures :once setup-test-db set-migrations-dir)

(deftest migrate-test
  (testing "No migrations, sets up the 'tri.migrations' table"
    ;; Arrange
    (let [tri-test "tri_test"
          ds (get-ds tri-test tri-test tri-test)]

      ;; Act
      (migrate! tri-test tri-test tri-test false)
      #_(println (jdbc/execute-one! ds ["SELECT version();"]))

      ;; Assert
      (is (some? (jdbc/execute-one! ds ["SELECT * FROM pg_catalog.pg_tables
                                        WHERE schemaname = 'tri'
                                        AND tablename = 'migrations';"])))))

  (testing "Completed migration is stored in the 'tri.migrations' table"
    ;; Arrange
    (let [tri-test "tri_test"
          ds       (get-ds tri-test tri-test tri-test)
          filename "01-create-users-table.sql"
          contents "CREATE TABLE users (id SERIAL PRIMARY KEY, username VARCHAR, password VARCHAR);"]

      (io/make-parents (str m/*migrations-dir* filename))
      (spit (str m/*migrations-dir* filename) contents)

      ;; Act
      (migrate! tri-test tri-test tri-test false)

      ;; Assert
      (let [result (jdbc/execute! ds ["SELECT * FROM tri.migrations;"] {:builder-fn as-unqualified-lower-maps})]
        (is (pos? (count result)))
        (is (= (-> result first :filename) filename))
        (is (= (-> result first :hash) (hash-str contents)))))))

(comment
  (run-tests 'triangulum.migrate-test)
  )
