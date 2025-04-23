(ns triangulum.migrate
  (:import java.io.File
           java.sql.Connection)
  (:require [clojure.java.io      :as io]
            [clojure.set          :refer [difference]]
            [clojure.string       :as str]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-lower-maps]]
            [triangulum.errors    :refer [nil-on-error]]
            [triangulum.security  :refer [hash-file]]
            [triangulum.utils     :refer [drop-sql-path]]))

;;; Constants

(def ^{:dynamic true :doc "Location of migrations dir"} *migrations-dir* "./src/sql/changes/")

;;; Helper Fns

(defn- migration-path [filename]
  (io/resource (drop-sql-path (.getPath ^File (io/file *migrations-dir* filename)))))

(defn- get-conn [host port database user user-pass]
  (jdbc/get-connection {:dbtype                "postgresql"
                        :dbname                database
                        :host                  host
                        :port                  port
                        :user                  user
                        :password              user-pass
                        :reWriteBatchedInserts true}))

(defn- get-migrations-dir []
  (io/make-parents *migrations-dir* "dummy.txt")
  *migrations-dir*)

(defmacro get-migration-files
  "An eval time list of the migration files.

   NOTE: This is a macro because we want to retain the file
   path information at AOT compile time so it's available at
   run time from a JAR."
  []
  (->> (get-migrations-dir)
       (io/file)
       (file-seq)
       (filter #(.isFile ^File %))
       (map #(.getName ^File %))
       (filter #(str/ends-with? % ".sql"))
       (sort)
       (vec)))

(defn- file-changed? [filename prev-file-hash]
  (nil-on-error (not= prev-file-hash (hash-file (migration-path filename)))))

(defn- get-completed-changes [db-conn]
  (let [completed     (jdbc/execute! db-conn ["SELECT filename, hash
                                              FROM tri.migrations
                                              ORDER BY created_date"]
                                     {:builder-fn as-unqualified-lower-maps})
        files-changed (filter #(file-changed? (:filename %) (:hash %)) completed)]
    (if (seq files-changed)
      (throw (ex-info (format "Error: Migrations have been modified: %s"
                              (str/join ", " (map :filename files-changed)))
                      {:files-changed files-changed}))
      (map :filename completed))))

(defn- set-completed! [db-conn filename]
  (jdbc/execute! db-conn ["INSERT INTO tri.migrations
                          (filename, hash)
                          VALUES (?, ?)"
                          filename
                          (hash-file (migration-path filename))]))

(defn- migration-error [e file-name new-changes]
  (str (format "Error: Did not complete migration %s and all migrations after:\n- " file-name)
       (str/join "\n- " (rest (drop-while #(not= file-name %) new-changes)))
       "\n\n"
       e))

(defn- apply-migration! [db-conn filename verbose?]
  (when verbose? (println "Migrating change:" filename))
  (let [migration (slurp (migration-path filename))]
    (when verbose? (println migration))
    (jdbc/with-transaction [tx db-conn]
      (jdbc/execute! tx [migration]))))

(defn- setup-migrations-table! [db-conn]
  (let [migration-exists? (jdbc/execute-one! db-conn ["SELECT * FROM pg_catalog.pg_tables
                                                      WHERE schemaname = 'tri'
                                                      AND tablename = 'migrations';"])]
    (when-not migration-exists?
      (jdbc/with-transaction [tx db-conn]
        (jdbc/execute! tx ["CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"])
        (jdbc/execute! tx ["CREATE SCHEMA IF NOT EXISTS tri;"])
        (jdbc/execute! tx ["CREATE TABLE IF NOT EXISTS tri.migrations (
                           migration_id uuid      PRIMARY KEY DEFAULT uuid_generate_v4(),
                           filename     varchar   NOT NULL,
                           hash         varchar   NOT NULL,
                           created_date timestamp DEFAULT now());"])))))

;;; Public fns

(defn migrate!
  "Performs the database migrations stored in the `src/sql/changes/` directory.
  Migrations must be stored in chronological order (e.g. `2021-02-28_add-users-table.sql`).

  Migrations run inside of a transaction block to ensure the entire migration is
  completed prior to being committed to the database.

  Currently, this tool does not support rollbacks.

  If a migration fails, all migrations which follow it will be cancelled.

  Migrations which have been completed are stored in a table `tri.migrations`,
  and include a SHA-256 hash of the migration file contents. If a migration has
  been altered, the migrations will fail. This is to ensure consistency as migrations
  are added."
  [host port database user user-pass verbose?]
  (when verbose? (println "Applying changes..."))

  (with-open [^Connection db-conn (get-conn host port database user user-pass)]
    (setup-migrations-table! db-conn)
    (let [all-files   (get-migration-files)
          completed   (get-completed-changes db-conn)
          new-changes (sort (difference (set all-files) (set completed)))]

      (when verbose? (println (format "Found %s new change files." (count new-changes))))

      (doseq [file new-changes]
        (try
          (apply-migration! db-conn file verbose?)
          (set-completed! db-conn file)
          (catch Exception e
            (throw (ex-info (migration-error (.getMessage e) file new-changes)
                            {:file        file
                             :new-changes new-changes}))))))

    (when verbose? (println "Completed migrations."))))
