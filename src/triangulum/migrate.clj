(ns triangulum.migrate
  (:import [java.io File])
  (:require [clojure.java.io     :as io]
            [clojure.edn         :as edn]
            [clojure.set         :refer [difference]]
            [clojure.java.shell  :as sh]
            [clojure.string      :as str]
            [next.jdbc           :as jdbc]
            [triangulum.cli      :refer [get-cli-options]]
            [triangulum.config   :refer [get-config]]
            [triangulum.security :refer [hash-str compare-sha256 hash-file]]
            [triangulum.utils    :refer [nil-on-error parse-as-sh-cmd format-str]]))

;; Constants
(def ^:private migrations-dir "./src/sql/changes")

;; Helper Fns

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
  (.mkdirs (File. migrations-dir))
  migrations-dir)

(defn- get-migration-files []
  (->> (get-migrations-dir)
       (io/file)
       (file-seq)
       (filter #(.isFile %))
       (map #(.getName %))
       (filter #(str/ends-with? % ".sql"))
       (sort)))

(defn- file-changed? [filename prev-file-hash]
  (compare-sha256 (hash-file (str migrations-dir "/" filename)) prev-file-hash))

(defn- get-completed-changes [ds]
  (let [completed     (jdbc/execute! ds ["SELECT filename, hash
                                          FROM triangulum.migrations
                                          ORDER BY created_date"])
        files-changed (filter #(file-changed? (:filename %) (:hashcode %)) completed)]
    (if (pos? (count files-changed))
      (throw (Exception. (format "Error: Migrations have been modified: %s"
                                 (str/join ", " (map :filename files-changed)))))
      {:completed (map :filename completed)})))

(defn- set-completed! [ds filename]
  (jdbc/execute! ds ["INSERT INTO triangulum.migrations
                     (filename, hash)
                     VALUES (?, ?)" filename (hash-file filename)]))

(defn- apply-migration! [ds verbose? filename]
  (when verbose? (println (format "Migrating change %s " filename)))
  (jdbc/with-transaction [tx ds]
    (jdbc/execute! tx [(slurp (str migrations-dir "/" filename))])))

;; Apply changes

(defn migrate! [database user user-pass verbose?]
  (when verbose? (println "Applying changes..."))
  (let [all-files   (get-migration-files)
        ds          (get-ds database user-pass user-pass)
        completed   (get-completed-changes ds)
        new-changes (sort (difference (set all-files) (set completed)))]

    (when verbose? (println (format "Found %s new change files." (count new-changes))))

    (when (nil? (nil-on-error (db-connected? ds)))
      (throw (Exception. "Error: Invalid database configuration. Please check your config.edn file.")))

    (when (pos? (count new-changes))
      (loop [file      (first new-changes)
             changes   (next new-changes)]
        (when (some? file)
          (apply-migration! ds verbose? file)
          (set-completed! ds file))
        (recur (first changes) (next changes))))

    (when verbose? (println "Completed migrations."))))

(defn- create-migrations-table [ds]
  (jdbc/execute! ds ["CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"])
  (jdbc/execute! ds ["CREATE SCHEMA IF NOT EXISTS triangulum;"])
  (jdbc/execute! ds ["CREATE TABLE IF NOT EXISTS triangulum.migrations (
                        migration_id uuid      PRIMARY KEY DEFAULT uuid_generate_v4 (),
                        filename     VARCHAR   NOT NULL,
                        hash         VARCHAR   NOT NULL,
                        created_date TIMESTAMP DEFAULT now());"]))

(comment

  (def db (get-config :database))
  (def ds (get-ds (:dbname db) (:user db) (:password db)))

  (create-migrations-table ds)

  (jdbc/execute! ds ["DROP TABLE IF EXISTS triangulum.migrations;"])
  (jdbc/execute! ds ["SELECT * FROM pg_catalog.pg_tables WHERE schemaname = 'triangulum';"])
  (jdbc/execute! ds ["SELECT * FROM triangulum.migrations;"])
  (jdbc/execute! ds ["CREATE SCHEMA IF NOT EXISTS triangulum;"])

  (jdbc/execute! ds ["SELECT routine_name FROM information_schema.routines WHERE routine_type = 'FUNCTION';"])

  (get-migration-files)

  (get-completed-changes ds)

  (def first-hash (hash-str (slurp "config.edn")))
  (def second-hash (hash-str (slurp "config.edn")))

  first-hash
  second-hash
  (= first-hash second-hash)
  (create-migrations-table ds)

  )
