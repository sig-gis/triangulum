(ns triangulum.migrate
  (:import [java.io File])
  (:require [clojure.java.io      :as io]
            [clojure.set          :refer [difference]]
            [clojure.string       :as str]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :refer [as-unqualified-lower-maps]]
            [triangulum.security  :refer [compare-sha256 hash-file]]
            [triangulum.utils     :refer [nil-on-error]]))

;; Constants
(def ^{:dynamic true} *migrations-dir* "./src/sql/changes/")

;; Helper Fns

(defn- migration-path [filename]
  (str *migrations-dir* "/" filename))

(defn- get-ds [database user user-pass]
  (jdbc/get-datasource {:dbtype                "postgresql"
                        :dbname                database
                        :user                  user
                        :password              user-pass
                        :reWriteBatchedInserts true}))

(defn- db-connected?
  ([ds]
   (jdbc/get-connection ds))
  ([database user user-pass]
   (jdbc/get-connection (get-ds database user user-pass))))

(defn- get-migrations-dir []
  (io/make-parents *migrations-dir*)
  *migrations-dir*)

(defn- get-migration-files []
  (->> (get-migrations-dir)
       (io/file)
       (file-seq)
       (filter #(.isFile %))
       (map #(.getName %))
       (filter #(str/ends-with? % ".sql"))
       (sort)))

(defn- file-changed? [filename prev-file-hash]
  (compare-sha256 (hash-file (migration-path filename)) prev-file-hash))

(defn- get-completed-changes [ds]
  (let [completed     (jdbc/execute! ds ["SELECT filename, hash
                                          FROM tri.migrations
                                          ORDER BY created_date"] {:builder-fn as-unqualified-lower-maps})
        files-changed (filter #(file-changed? (:filename %) (:hashcode %)) completed)]
    (if (pos? (count files-changed))
      (throw (Exception. (format "Error: Migrations have been modified: %s"
                                 (str/join ", " (map :filename files-changed)))))
      (map :filename completed))))

(defn- set-completed! [ds filename]
  (jdbc/execute! ds ["INSERT INTO tri.migrations
                     (filename, hash)
                     VALUES (?, ?)" filename (hash-file (str *migrations-dir* "/" filename))]))

(defn- migration-error [e filename remaining-files]
  (str (format "Error: Did not complete migration %s and all migrations after:\n" filename)
       (str/join "\n- " remaining-files)
       "\n\n"
       e))

(defn- apply-migration! [ds filename verbose?]
  (when verbose? (println (format "Migrating change %s " filename)))
  (let [transaction #(jdbc/with-transaction [tx ds]
                       (jdbc/execute! tx [(slurp (str *migrations-dir* "/" filename))]))
        result (try
                 (transaction)
                 (catch Exception e {:error (ex-message e)}))]
    (if (:error result)
      result
      (when verbose? (println (format "Completed migration %s" filename))))))

(defn- setup-migrations-table! [ds]
  (let [migration-exists? (jdbc/execute-one! ds ["SELECT * FROM pg_catalog.pg_tables
                                                 WHERE schemaname = 'tri'
                                                 AND tablename = 'migrations';"])]
    (when-not migration-exists?
      (jdbc/execute! ds ["CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"])
      (jdbc/execute! ds ["CREATE SCHEMA IF NOT EXISTS tri;"])
      (jdbc/execute! ds ["CREATE TABLE IF NOT EXISTS tri.migrations (
                         migration_id uuid      PRIMARY KEY DEFAULT uuid_generate_v4 (),
                         filename     VARCHAR   NOT NULL,
                         hash         VARCHAR   NOT NULL,
                         created_date TIMESTAMP DEFAULT now());"]))))

;; Public fns

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
  [database user user-pass verbose?]
  (when verbose? (println "Applying changes..."))

  (when (nil? (nil-on-error (db-connected? database user user-pass)))
    (throw (Exception. "Error: Invalid database configuration. Please check your config.edn file.")))

  (let [all-files   (get-migration-files)
        ds          (get-ds database user user-pass)
        _           (setup-migrations-table! ds)
        completed   (get-completed-changes ds)
        new-changes (sort (difference (set all-files) (set completed)))]

    (when verbose? (println (format "Found %s new change files." (count new-changes))))

    (when (pos? (count new-changes))
      (loop [file      (first new-changes)
             changes   (next new-changes)]
        (when (some? file)
          (if-let [{error :error} (apply-migration! ds file verbose?)]
            (throw (Exception. (migration-error error file changes)))
            (do
              (set-completed! ds file)
              (recur (first changes) (next changes)))))))

    (when verbose? (println "Completed migrations."))))
