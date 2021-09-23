(ns triangulum.build-db
  (:import java.io.File)
  (:require [clojure.java.io    :as io]
            [clojure.edn        :as edn]
            [clojure.set        :refer [difference]]
            [clojure.java.shell :as sh]
            [clojure.string     :as str]
            [next.jdbc :as jdbc]
            [triangulum.cli    :refer [get-cli-options]]
            [triangulum.config :refer [get-config]]
            [triangulum.utils  :refer [nil-on-error parse-as-sh-cmd format-str]]))

(def ^:private path-env (System/getenv "PATH"))

;; Helper functions

;; TODO consolidate sh-wrapper functions
(defn- sh-wrapper [dir env verbose & commands]
  (sh/with-sh-dir dir
    (sh/with-sh-env (merge {:PATH path-env} env)
      (reduce (fn [acc cmd]
                (let [{:keys [out err]} (apply sh/sh (parse-as-sh-cmd cmd))]
                  (str acc (when verbose out) err)))
              ""
              commands))))

;; Namespace file sorting functions

(defn- get-sql-files [dir-name]
  (let [file (io/file dir-name)]
    (if (.exists file)
      (filter (fn [^File f] (str/ends-with? (.getName f) ".sql"))
              (file-seq file))
      (do (println "Warning:" dir-name "is not found.")
          []))))

(defn- extract-toplevel-sql-comments [file]
  (->> (io/reader file)
       (line-seq)
       (take-while #(str/starts-with? % "-- "))))

(defn- parse-sql-comments [comments]
  (reduce (fn [acc cur]
            (let [[k v] (-> cur
                            (subs 3)
                            (str/lower-case)
                            (str/split #":"))]
              (assoc acc (keyword (str/trim k)) (str/trim v))))
          {}
          (filter #(str/includes? % ":") comments)))

#_{:clj-kondo/ignore [:shadowed-var]}
(defn- params-to-dep-tree [file-params]
  (reduce (fn [dep-tree {:keys [namespace requires]}]
            (assoc dep-tree
                   namespace
                   (set (when requires (remove str/blank? (str/split requires #"[, ]"))))))
          {}
          file-params))

;; TODO, with more namespaces, which don't have a linear dependency
;;       the sort method used doesn't end up comparing all values with each other.
;; Comparing dependency count works for now, but a true solution would be
;;       a breadth first tree walk.
(defn- requires? [[_ deps1] [ns2 deps2]]
  (or (contains? deps1 ns2)
      (> (count deps1) (count deps2))))

(defn- topo-sort-namespaces [dep-tree]
  (map first
       (sort (fn [file1 file2]
               (cond (requires? file1 file2)  1
                     (requires? file2 file1) -1
                     :else                    0))
             dep-tree)))

(defn- warn-namespace [parsed file]
  (when-not (:namespace parsed)
    (println "Warning: Invalid or missing '-- NAMESPACE:' tag for file" file))
  parsed)

(defn- topo-sort-files-by-namespace [dir-name]
  (let [sql-files   (get-sql-files dir-name)
        file-params (map #(-> %
                              (extract-toplevel-sql-comments)
                              (parse-sql-comments)
                              (warn-namespace %))
                         sql-files)
        ns-to-files (zipmap (map :namespace file-params)
                            (map (fn [^File f] (.getPath f)) sql-files))
        dep-tree    (params-to-dep-tree file-params)
        sorted-ns   (topo-sort-namespaces dep-tree)]
    (map ns-to-files sorted-ns)))

;; Build functions

(def ^:private folders {:tables    "./src/sql/tables"
                        :functions "./src/sql/functions"
                        :defaults  "./src/sql/default_data"
                        :dev       "./src/sql/dev_data"})

(defn- load-folder [sql-type database user user-pass verbose]
  (let [folder (sql-type folders)]
    (println (str "Loading " folder "..."))
    (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
              (topo-sort-files-by-namespace folder))
         (apply sh-wrapper "./" {:PGPASSWORD user-pass} verbose)
         (println))))

(defn- build-everything [database user user-pass admin-pass dev-data? verbose]
  (println "Building database...")
  (let [file (io/file "./src/sql/create_db.sql")]
    (if (.exists file)
      (do (->> (sh-wrapper "./src/sql"
                           {:PGPASSWORD admin-pass}
                           verbose
                           (format "psql -h localhost --set=database=%s -U postgres -f create_db.sql"
                                   database))
               (println))
          (load-folder :tables    database user user-pass verbose)
          (load-folder :functions database user user-pass verbose)
          (load-folder :defaults  database user user-pass verbose)
          (when dev-data?
            (load-folder :dev database user user-pass verbose)))
      (println "Error file ./src/sql/create_db.sql is missing."))))

;; Apply changes

(def ^:private migrations-dir "./src/sql/changes")
(def ^:private change-file    ".build-db-changes")
(def ^:private rollback?      (atom false))

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
       (set)))

(defn- get-completed-changes []
  (let [file (io/file change-file)]
    (if (.exists file)
      (-> file (slurp) (edn/read-string))
      #{})))

(defn- set-completed-changes [new-changes]
  (spit change-file (prn-str new-changes)))

(defn- get-ds [database user user-pass]
  (jdbc/get-datasource {:dbtype                "postgresql"
                        :dbname                database
                        :user                  user
                        :password              user-pass
                        :reWriteBatchedInserts true}))

(defn- check-conn [database user user-pass]
  (jdbc/get-connection (get-ds database user user-pass)))

(defn- apply-change-sql-file [database user user-pass verbose? file]
  (when verbose? (println (format "Migrating change %s " file)))
  (let [ds (get-ds database user user-pass)]
    (jdbc/with-transaction [tx ds]
      (jdbc/execute! tx [(slurp (str migrations-dir "/" file))]))))

(defn- apply-changes [database user user-pass verbose?]
  (when verbose? (println "Applying changes..."))
  (let [all-changes       (get-migration-files)
        completed-changes (get-completed-changes)
        new-changes       (sort (difference all-changes completed-changes))]

    (when verbose? (println (format "Found %s new change files." (count new-changes))))

    (when (nil? (nil-on-error (check-conn database user user-pass)))
      (throw (Exception. "Error: Invalid database configuration. Please check your config.edn file.")))

    (when (< 0 (count new-changes))
      (loop [file      (first new-changes)
             changes   (rest new-changes)
             completed completed-changes]
        (when-not (nil? file)
          (let [result (nil-on-error (apply-change-sql-file database user user-pass verbose? file))]
            (when-not (nil? result)
              (set-completed-changes (conj completed file))
              (when-not (empty? changes)
                (recur (first changes) (rest changes) (conj completed file))))))))
    (when verbose? (println "Completed migrations."))))

(defn- reset-changes []
  (println "Resetting change file...")
  (set-completed-changes #{}))

(defn- last-change []
  (println "Last change:" (-> (get-completed-changes) (sort) (last))))

;; Backup / restore functions

(defn- read-file-tag [file]
  (with-open [is (io/input-stream file)]
    (let [array (byte-array 5)]
      (.read is array 0 5)
      (String. array))))

(defn- run-backup [database file admin-pass verbose]
  (println "Backing up database...")
  (sh-wrapper "./"
              {:PGPASSWORD admin-pass}
              verbose
              (format-str "pg_dump -U postgres -d %d --format=custom --compress=4 --file=%f"
                          database
                          file)))

(defn- run-restore [file admin-pass verbose]
  ;; TODO check database against 'pg_restore --list file'
  (println "Restoring database...")
  (if (= "PGDMP" (read-file-tag file))
    (sh-wrapper "./"
                {:PGPASSWORD admin-pass}
                verbose
                (str "pg_restore -U postgres -d postgres --clean --if-exists --create --jobs=12 "
                     file))
    (println "Invalid .dump file.")))

(def ^:private cli-options
  {:dbname     ["-d" "--dbname DB"           "Database name."]
   :dev-data   ["-x" "--dev-data"            "Load dev data."]
   :file       ["-f" "--file FILE"           "File used for backup and restore."]
   :admin-pass ["-a" "--admin-pass PASSWORD" "Admin password for the postgres account."]
   :user       ["-u" "--user USER"           "User for the database. Defaults to the same as the database name."]
   :password   ["-p" "--password PASSWORD"   "Password for the database. Defaults to the same as the database name."]
   :verbose    ["-v" "--verbose"             "Print verbose PostgreSQL output."]})

(def ^:private cli-actions
  {:backup        {:description "Create a .dump backup file using pg_dump."
                   :requires    [:dbname :file]}
   :build-all     {:description "Build / rebuild the entire data base."
                   :requires    [:dbname]}
   :functions     {:description "Build / rebuild all functions."
                   :requires    [:dbname]}
   :restore       {:description "Restore a database from a .dump file created by pg_dump."
                   :requires    [:file]}
   :apply-changes {:description "Applies the migration files under `src/sql/changes` in chronological order."
                   :requires    [:dbname :user :password]}
   :reset-changes {:description "Resets the existing change file, which allows all migrations to be re-run."
                   :requires    []}
   :last-change   {:description "Returns the last entry (in chronological order) from the change file."
                   :requires    []}})

(defn -main
  "A set of tools for building and maintaining the project database with Postgres."
  [& args]
  (let [{:keys [action options]} (get-cli-options args
                                                  cli-options
                                                  cli-actions
                                                  "build-db"
                                                  (get-config :database))
        {:keys [dbname dev-data file password admin-pass user verbose]} options]
    (case action
      :build-all (build-everything dbname
                                   (or user dbname)
                                   (or password dbname) ; user-pass
                                   admin-pass
                                   dev-data
                                   verbose)
      :functions (load-folder :functions
                              dbname
                              (or user dbname)
                              (or password dbname) ; user-pass
                              verbose)
      :backup    (run-backup dbname file admin-pass verbose)
      :restore   (run-restore file admin-pass verbose)
      :apply-changes (apply-changes dbname
                                    (or user dbname)
                                    (or password dbname) ; user-pass
                                    verbose)
      :reset-changes (reset-changes)
      :last-change   (last-change)
      nil))
  (shutdown-agents))
