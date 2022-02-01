(ns triangulum.migrate-test
  (:require [clojure.test         :refer [is deftest testing use-fixtures]]
            [clojure.java.io      :as io]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-lower-maps]]
            [triangulum.security  :refer [hash-str]]
            [triangulum.migrate   :refer [migrate!] :as m]
            [triangulum.utils     :refer [delete-recursively]]))

;; Debugging
(def ^:private verbose? false)
(def ^:private tri-test "tri_test")

;; Helpers
(defn- get-conn [database user user-pass]
  (println "Attempting to connect to" database user user-pass)
  (jdbc/get-connection {:dbtype                "postgresql"
                        :dbname                database
                        :user                  user
                        :password              user-pass
                        :reWriteBatchedInserts true}))

(defn- get-admin-conn []
  (get-conn (or (System/getenv "PGDATABASE") "postgres")
            (or (System/getenv "PGUSERNAME") "postgres")
            (or (System/getenv "PGPASSWORD") "")))

(defn- get-migrations [db-conn]
  (jdbc/execute! db-conn
                 ["SELECT * FROM tri.migrations;"]
                 {:builder-fn as-unqualified-lower-maps}))

;; Fixtures
(defn- set-migrations-dir [f]
  (when (.exists (io/file "tmp")) (delete-recursively "tmp"))
  (with-bindings {#'m/*migrations-dir* "tmp/sql/changes/"}
    (f))
  (delete-recursively "tmp"))

(def ^:private create-db ["CREATE ROLE tri_test WITH LOGIN CREATEDB PASSWORD 'tri_test';"
                          "CREATE DATABASE tri_test WITH OWNER tri_test;"])
(def ^:private drop-db   ["DROP DATABASE IF EXISTS tri_test;" "DROP ROLE IF EXISTS tri_test"])

(defn- setup-test-db [f]
  (with-open [con (get-admin-conn)]
    (doseq [q (concat drop-db create-db)]
      (jdbc/execute-one! con [q])))
  (f)
  (with-open [con (get-admin-conn)]
    (doseq [q drop-db]
      (jdbc/execute-one! con [q]))))

(use-fixtures :once setup-test-db set-migrations-dir)

(deftest ^:db migrate-test
  (testing "No migrations, sets up the 'tri.migrations' table"
    ; Arrange

    ; Act
    (migrate! tri-test tri-test tri-test verbose?)

    ; Assert
    (with-open [con (get-conn tri-test tri-test tri-test)]
      (is (some? (jdbc/execute-one! con ["SELECT * FROM pg_catalog.pg_tables
                                         WHERE schemaname = 'tri'
                                         AND tablename = 'migrations';"])))))

  (testing "Completed migration is stored in the 'tri.migrations' table"
    ; Arrange
    (let [filename "01-create-users-table.sql"
          contents "CREATE TABLE users (id SERIAL PRIMARY KEY, username VARCHAR, password VARCHAR);"]

      (io/make-parents (str m/*migrations-dir* filename))
      (spit (str m/*migrations-dir* filename) contents)

      ; Act
      (migrate! tri-test tri-test tri-test verbose?)

      ; Assert
      (with-open [con (get-conn tri-test tri-test tri-test)]
        (let [migrations (get-migrations con)]
          (is (pos? (count migrations)))
          (is (= (-> migrations first :filename) filename))
          (is (= (-> migrations first :hash) (hash-str contents)))))))

  (testing "Migrations are not run more than once."
    ; Arrange
    (spit (str m/*migrations-dir* "02-add-column-city-users.sql")
          "ALTER TABLE users ADD COLUMN city VARCHAR;")

    ; Act
    (doseq [_ (range 0 5)]
      (migrate! tri-test tri-test tri-test verbose?))

    ; Assert
    (with-open [con (get-conn tri-test tri-test tri-test)]
      (let [migrations (get-migrations con)]
        (is (= (count migrations) 2)))
      (let [columns (jdbc/execute! con ["SELECT column_name FROM information_schema.columns
                                        WHERE table_schema = 'public'
                                        AND table_name = 'users'"])]
        (is (= (count columns) 4)))))

  (testing "Migrations do not continue once an error has been reached."
    ; Arrange
    (spit (str m/*migrations-dir* "03-ERROR.sql")
          "ALTER TABLE users ADD state;")

    (spit (str m/*migrations-dir* "04-add-table-pets.sql")
          "CREATE TABLE pets ( id SERIAL PRIMARY KEY, pet_name VARCHAR);")

    ; Act
    (is (thrown? Exception (migrate! tri-test tri-test tri-test verbose?)))

    ; Assert
    (with-open [con (get-conn tri-test tri-test tri-test)]
      (let [migrations (get-migrations con)
            columns    (jdbc/execute! con ["SELECT column_name FROM information_schema.columns
                                           WHERE table_schema = 'public'
                                           AND table_name = 'users'"])]
        (is (= (count migrations) 2))
        (is (= (count columns) 4)))))

  (testing "Errors out when a migration is modified."
    ; Arrange
    (spit (str m/*migrations-dir* "01-create-users-table.sql") "CREATE TABLE users (id SERIAL PRIMARY KEY);")

    ; Act/Assert
    (is (thrown? Exception (migrate! tri-test tri-test tri-test false)))))
