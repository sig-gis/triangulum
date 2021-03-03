(ns triangulum.build-db
  (:require [clojure.java.io    :as io]
            [clojure.java.shell :as sh]
            [clojure.string     :as str]
            [clojure.tools.cli  :refer [parse-opts]]
            [triangulum.utils   :refer [parse-as-sh-cmd format-%]]))

(def path-env (System/getenv "PATH"))

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
  (->> (io/file dir-name)
       (file-seq)
       (filter #(str/ends-with? (.getName %) ".sql"))))

(defn- extract-toplevel-sql-comments [file]
  (->> (io/reader file)
       (line-seq)
       (take-while #(str/starts-with? % "-- "))))

(defn- parse-sql-comments [comments]
  (reduce (fn [params comment]
            (let [[k v] (-> comment
                            (subs 3)
                            (str/lower-case)
                            (str/split #":"))]
              (assoc params (keyword (str/trim k)) (str/trim v))))
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
                            (map #(.getPath %) sql-files))
        dep-tree    (params-to-dep-tree file-params)
        sorted-ns   (topo-sort-namespaces dep-tree)]
    (map ns-to-files sorted-ns)))

;; Build functions

(defn- load-tables [database user verbose]
  (println "Loading tables...")
  (->> (map #(format-% "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/tables"))
       (apply sh-wrapper "./" {:PGPASSWORD user} verbose)
       (println)))

(defn- load-functions [database user verbose]
  (println "Loading functions...")
  (->> (map #(format-% "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/functions"))
       (apply sh-wrapper "./" {:PGPASSWORD user} verbose)
       (println)))

(defn- load-default-data [database user verbose]
  (println "Loading default data...")
  (->> (map #(format-% "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/default_data"))
       (apply sh-wrapper "./" {:PGPASSWORD user} verbose)
       (println)))

(defn- build-everything [database user password verbose]
  (println "Building database...")
  (->> (sh-wrapper "/mshr/github/pyregence/src/sql"
                   {:PGPASSWORD password}
                   verbose
                   (str "psql -h localhost -U postgres -f create_db.sql"))
       (println))
  (load-tables       database user verbose)
  (load-functions    database user verbose)
  (load-default-data database user verbose))

;; Backup / restore functions

(defn- read-file-tag [file]
  (with-open [is (io/input-stream file)]
    (let [b-ary (byte-array 5)]
      (.read is b-ary 0 5)
      (String. b-ary))))

(defn- run-backup
  "Creates a Postgres backup for the given data base.
   The format custom is used to create a compressed binary .dump file."
  [database file password verbose]
  (println "Backing up database...")
  (if (nil? file)
    (println "Please specify a file destination with -f. The provided file name should use the extension .dump.")
    (sh-wrapper "./"
                {:PGPASSWORD password}
                verbose
                (format-% "pg_dump -U postgres -d %d --format=custom --compress=4 --file=%f"
                          database
                          file))))

(defn- run-restore
  "Restores a Postgres backup from the compressed binary .dump file.
   Database is not required because the .dump file contains the information
   on where to restore the database."
  [_ file password verbose]
  ;; TODO check database against 'pg_restore --list file'
  (println "Restoring database...")
  (cond
    (nil? file)
    (println "Please specify a .dump file to be restored with -f.")

    (= "PGDMP" (read-file-tag file))
    (sh-wrapper "./"
                {:PGPASSWORD password}
                verbose
                (str "pg_restore -U postgres -d postgres --clean --if-exists --create --jobs=12 "
                     file))
    :else
    (println "Invalid .dump file.")))

(def cli-options
  [["-d" "--database DB"       "Database name. Required."
    :missing "You must provide a database name."]
   ["-f" "--file FILE"         "File used for backup and restore."]
   ["-p" "--password PASSWORD" "Admin password for the postgres account."]
   ["-u" "--user USER"         "User for the database. Defaults to the same as the database name."]
   ["-v" "--verbose"           "Print verbose PostgreSQL output."]])

(def actions
  {:backup    "Create backup file using pg_dump."
   :build-all "Build / rebuild the entire data base."
   :functions "Build / rebuild all functions."
   :restore   "Restore a database created by pg_dump."})

(def usage-str
  (->> (concat ["Usage: clojure -M:build-db [options] action"
                ""
                "Options:"]
               (map (fn [[shrt lng description]]
                      (format "  %s, %-23s%s" shrt lng description))
                    cli-options)
               [""
                "Actions:"]
               (map (fn [[action description]]
                      (format "   %-26s%s" (name action) description))
                    actions))
       (str/join \newline)))

(defn check-errors [errors arguments action]
  (cond
    (seq errors)
    (str/join \newline errors)

    (= 0 (count arguments))
    "You must select an action."

    (< 1 (count arguments))
    "You only select one action at a time."

    (not (get actions action))
    "Invalid action selection."))

(defn -main [& args]
  (let [{:keys [arguments options errors]} (parse-opts args cli-options)
        {:keys [database file password user verbose]} options
        action    (keyword (first arguments))
        error-msg (check-errors errors arguments action)]
    (cond
      error-msg
      (do
        (println "Error: " error-msg "\n")
        (println usage-str))

      (= :build-all action)
      (build-everything database (or user database) password verbose)

      (= :functions action)
      (load-functions database (or user database) verbose)

      (= :backup action)
      (run-backup database file password verbose)

      (= :restore action)
      (run-restore database file password verbose)))
  (shutdown-agents))
