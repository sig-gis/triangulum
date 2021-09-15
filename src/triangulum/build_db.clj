(ns triangulum.build-db
  (:import java.io.File)
  (:require [clojure.java.io    :as io]
            [clojure.java.shell :as sh]
            [clojure.string     :as str]
            [triangulum.cli    :refer [get-cli-options]]
            [triangulum.config :refer [get-config]]
            [triangulum.utils  :refer [parse-as-sh-cmd format-str]]))

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

(defn- load-tables [database user user-pass verbose]
  (println "Loading tables...")
  (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/tables"))
       (apply sh-wrapper "./" {:PGPASSWORD user-pass} verbose)
       (println)))

(defn- load-functions [database user user-pass verbose]
  (println "Loading functions...")
  (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/functions"))
       (apply sh-wrapper "./" {:PGPASSWORD user-pass} verbose)
       (println)))

(defn- load-default-data [database user user-pass verbose]
  (println "Loading default data...")
  (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/default_data"))
       (apply sh-wrapper "./" {:PGPASSWORD user-pass} verbose)
       (println)))

(defn- build-everything [database user user-pass admin-pass verbose]
  (println "Building database...")
  (let [file (io/file "./src/sql/create_db.sql")]
    (if (.exists file)
      (do (->> (sh-wrapper "./src/sql"
                           {:PGPASSWORD admin-pass}
                           verbose
                           (format "psql -h localhost --set=database=%s -U postgres -f create_db.sql"
                                   database))
               (println))
          (load-tables       database user user-pass verbose)
          (load-functions    database user user-pass verbose)
          (load-default-data database user user-pass verbose))
      (println "Error file ./src/sql/create_db.sql is missing."))))

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
  {:dbname     ["-d" "--dbname DB"         "Database name."]
   :file       ["-f" "--file FILE"           "File used for backup and restore."]
   :admin-pass ["-a" "--admin-pass PASSWORD" "Admin password for the postgres account."]
   :user       ["-u" "--user USER"           "User for the database. Defaults to the same as the database name."]
   :password   ["-p" "--password PASSWORD"   "Password for the database. Defaults to the same as the database name."]
   :verbose    ["-v" "--verbose"             "Print verbose PostgreSQL output."]})

(def ^:private cli-actions
  {:backup    {:description "Create a .dump backup file using pg_dump."
               :requires    [:dbname :file]}
   :build-all {:description "Build / rebuild the entire data base."
               :requires    [:dbname]}
   :functions {:description "Build / rebuild all functions."
               :requires    [:dbname]}
   :restore   {:description "Restore a database from a .dump file created by pg_dump."
               :requires    [:file]}})

(defn -main
  "A set of tools for building and maintaining the project database with Postgres."
  [& args]
  (let [{:keys [action options]} (get-cli-options args
                                                  cli-options
                                                  cli-actions
                                                  "build-db"
                                                  (get-config :database))
        {:keys [dbname file password admin-pass user verbose]} options]
    (case action
      :build-all (build-everything dbname
                                   (or user dbname)
                                   (or password dbname) ; user-pass
                                   admin-pass
                                   verbose)
      :functions (load-functions dbname
                                 (or user dbname)
                                 (or password dbname) ; user-pass
                                 verbose)
      :backup    (run-backup dbname file admin-pass verbose)
      :restore   (run-restore file admin-pass verbose)
      nil))
  (shutdown-agents))
