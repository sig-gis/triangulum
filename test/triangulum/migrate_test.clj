(ns triangulum.migrate-test
  (:require [clojure.java.io      :as io]
            [clojure.test         :refer [is deftest testing use-fixtures]]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-lower-maps]]
            [triangulum.security  :refer [hash-digest]]
            [triangulum.migrate   :refer [migrate!] :as m]
            [triangulum.utils     :refer [delete-recursively]]))

;; Debugging
(def ^:private verbose? false)
(def ^:private tri-test "tri_test")

;; Helpers
(defn- get-conn [config]
  (try
    (jdbc/get-connection (merge {:dbtype "postgresql" :reWriteBatchedInserts true} config))
    (catch Exception _ (println "Unable to connect to db using:" config))))

(defn- get-admin-conn []
  (get-conn {:host     (or (System/getenv "PGHOST") "localhost")
             :port     (or (System/getenv "PGPORT") 5432)
             :dbname   (or (System/getenv "PGDATABASE") "postgres")
             :user     (or (System/getenv "PGUSERNAME") "postgres")
             :password (or (System/getenv "PGPASSWORD") "")}))

(defn- get-tri-test-conn []
  (get-conn {:host     (or (System/getenv "PGHOST") "localhost")
             :port     (or (System/getenv "PGPORT") 5432)
             :dbname   tri-test
             :user     tri-test
             :password tri-test}))

(defn- get-migrations [db-conn]
  (jdbc/execute! db-conn
                 ["SELECT * FROM tri.migrations;"]
                 {:builder-fn as-unqualified-lower-maps}))

;; Fixtures
(defn- set-migrations-dir [f]
  (when (.exists (io/file "tmp")) (delete-recursively "tmp"))
  (with-bindings {#'m/*migrations-dir* "tmp/sql/changes/"}
    (f))
  (when (.exists (io/file "tmp")) (delete-recursively "tmp")))

(def ^:private create-db ["CREATE ROLE tri_test WITH LOGIN CREATEDB PASSWORD 'tri_test';"
                          "CREATE DATABASE tri_test WITH OWNER tri_test;"])
(def ^:private drop-db   ["DROP DATABASE IF EXISTS tri_test;"
                          "DROP ROLE IF EXISTS tri_test;"])

(defn- setup-test-db [f]
  (when-let [conn (get-admin-conn)]
    (try
      (doseq [q (concat drop-db create-db)]
        (jdbc/execute-one! conn [q]))
      (finally
        (.close conn))))
  (f)
  (when-let [conn (get-admin-conn)]
    (try
      (doseq [q drop-db]
        (jdbc/execute-one! conn [q]))
      (finally
        (.close conn)))))

(use-fixtures :once setup-test-db set-migrations-dir)

(deftest ^:db migrate-test
  (testing "No migrations, sets up the 'tri.migrations' table"
    ; Arrange

    ; Act
    (migrate! tri-test tri-test tri-test verbose?)

    ; Assert
    (with-open [conn (get-tri-test-conn)]
      (is (some? (jdbc/execute-one! conn ["SELECT * FROM pg_catalog.pg_tables
                                         WHERE schemaname = 'tri'
                                         AND tablename = 'migrations';"])))))

  (testing "Completed migration is stored in the 'tri.migrations' table"
    ; Arrange
    (let [filename "01-create-users-table.sql"
          contents "CREATE TABLE users (id SERIAL PRIMARY KEY, username varchar, password varchar);"]

      (io/make-parents (str m/*migrations-dir* filename))
      (spit (str m/*migrations-dir* filename) contents)

      ; Act
      (migrate! tri-test tri-test tri-test verbose?)

      ; Assert
      (with-open [conn (get-tri-test-conn)]
        (let [migrations (get-migrations conn)]
          (is (pos? (count migrations)))
          (is (= (-> migrations first :filename) filename))
          (is (= (-> migrations first :hash) (hash-digest contents)))))))

  (testing "Migrations are not run more than once."
    ; Arrange
    (spit (str m/*migrations-dir* "02-add-column-city-users.sql")
          "ALTER TABLE users ADD COLUMN city VARCHAR;")

    ; Act
    (dotimes [_ 5]
      (migrate! tri-test tri-test tri-test verbose?))

    ; Assert
    (with-open [conn (get-tri-test-conn)]
      (let [migrations (get-migrations conn)]
        (is (= (count migrations) 2)))
      (let [columns (jdbc/execute! conn ["SELECT column_name FROM information_schema.columns
                                        WHERE table_schema = 'public'
                                        AND table_name = 'users'"])]
        (is (= (count columns) 4)))))

  (testing "Migrations do not continue once an error has been reached."
    ; Arrange
    (spit (str m/*migrations-dir* "03-ERROR.sql")
          "ALTER TABLE users ADD state;")

    (spit (str m/*migrations-dir* "04-add-table-pets.sql")
          "CREATE TABLE pets (id SERIAL PRIMARY KEY, pet_name varchar);")

    ; Act
    (is (thrown? Exception (migrate! tri-test tri-test tri-test verbose?)))

    ; Assert
    (with-open [conn (get-tri-test-conn)]
      (let [migrations (get-migrations conn)
            columns    (jdbc/execute! conn ["SELECT column_name FROM information_schema.columns
                                           WHERE table_schema = 'public'
                                           AND table_name = 'users'"])]
        (is (= (count migrations) 2))
        (is (= (count columns) 4)))))

  (testing "Errors out when a migration is modified."
    ; Arrange
    (spit (str m/*migrations-dir* "01-create-users-table.sql") "CREATE TABLE users (id SERIAL PRIMARY KEY);")

    ; Act/Assert
    (is (thrown? Exception (migrate! tri-test tri-test tri-test false)))))
