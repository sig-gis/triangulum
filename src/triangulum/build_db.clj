(ns triangulum.build-db
  (:require [clojure.java.io    :as io]
            [clojure.java.shell :as sh]
            [clojure.string     :as str]
            [clojure.tools.cli  :refer [parse-opts]]
            [triangulum.utils   :refer [parse-as-sh-cmd format-str]]))

(def ^:private path-env (System/getenv "PATH"))

;; SH helper function

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
                            (map #(.getPath %) sql-files))
        dep-tree    (params-to-dep-tree file-params)
        sorted-ns   (topo-sort-namespaces dep-tree)]
    (map ns-to-files sorted-ns)))

;; Build functions

(defn- load-tables [database user verbose]
  (println "Loading tables...")
  (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/tables"))
       (apply sh-wrapper "./" {:PGPASSWORD user} verbose)
       (println)))

(defn- load-functions [database user verbose]
  (println "Loading functions...")
  (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/functions"))
       (apply sh-wrapper "./" {:PGPASSWORD user} verbose)
       (println)))

(defn- load-default-data [database user verbose]
  (println "Loading default data...")
  (->> (map #(format-str "psql -h localhost -U %u -d %d -f %f" user database %)
            (topo-sort-files-by-namespace "./src/sql/default_data"))
       (apply sh-wrapper "./" {:PGPASSWORD user} verbose)
       (println)))

(defn- build-everything [database user verbose]
  (println "Building database...")
  (print "Please enter the postgres user's password:")
  (flush)
  (let [password (String/valueOf (.readPassword (System/console)))]
    (->> (sh-wrapper "./src/sql"
                     {:PGPASSWORD password}
                     verbose
                     (str "psql -h localhost -U postgres -f create_db.sql"))
         (println)))
  (load-tables       database user verbose)
  (load-functions    database user verbose)
  (load-default-data database user verbose))

;; CLI param parsing

(def ^:private cli-options
  [["-d" "--database DB" "Database name. Required"
    :missing "You must provide a database name."]
   ["-u" "--user USER" "User for the database. Defaults to the same as the database name."]
   ["-v" "--verbose" "Print verbose PostgreSQL output."]])

(def ^:private actions
  {:build-all "Build / rebuild the entire data base."
   :functions "Build / rebuild all functions."})

(defn- print-usage [options-summary]
  (->> (map (fn [[action description]]
                       (str (name action) "    " description))
                     actions)
                (concat ["Usage: clojure -M:build-db [options] action"
                         ""
                         "Options:"
                         options-summary
                         ""
                         "Actions:"])
                (str/join \newline)))

(defn- check-errors [errors arguments action]
  (cond
    (seq errors)
    (str/join \newline errors)

    (not (and (= 1 (count arguments))
              (get actions action)))
    "Invalid action selection."))

(defn -main
  "A set of tools for building and maintaining the project database with Postgres"
  [& args]
  (let [{:keys [arguments options summary errors]} (parse-opts args cli-options)
        {:keys [database user verbose]} options
        action    (keyword (first arguments))
        error-msg (check-errors errors arguments action)]
    (cond
      error-msg
      (do
        (println "Error: " error-msg "\n")
        (print-usage summary))

      (= :build-all action)
      (build-everything database (or user database) verbose)

      (= :functions action)
      (load-functions database (or user database) verbose)))
  (shutdown-agents))
