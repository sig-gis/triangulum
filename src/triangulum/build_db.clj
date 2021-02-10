(ns triangulum.build-db
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [triangulum.utils :refer [parse-as-sh-cmd]]))

(def path-env (System/getenv "PATH"))

;; SH helper function

(defn sh-wrapper [dir env verbose & commands]
  (sh/with-sh-dir dir
    (sh/with-sh-env (merge {:PATH path-env} env)
      (reduce (fn [acc cmd] (let [{:keys [out err]} (apply sh/sh (parse-as-sh-cmd cmd))] (str acc (when verbose out) err)))
              ""
              commands))))

;; Namespace file sorting functions

(defn get-sql-files [dir-name]
  (->> (io/file dir-name)
       (file-seq)
       (filter #(str/ends-with? (.getName %) ".sql"))))

(defn extract-toplevel-sql-comments [file]
  (->> (io/reader file)
       (line-seq)
       (take-while #(str/starts-with? % "-- "))))

(defn parse-sql-comments [comments]
  (reduce (fn [params comment]
            (let [[k v] (-> comment
                            (subs 3)
                            (str/lower-case)
                            (str/split #":"))]
              (assoc params (keyword (str/trim k)) (str/trim v))))
          {}
          (filter #(str/includes? % ":") comments)))

(defn params-to-dep-tree [file-params]
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
(defn requires? [[_ deps1] [ns2 deps2]]
  (or (contains? deps1 ns2)
      (> (count deps1) (count deps2))))

(defn topo-sort-namespaces [dep-tree]
  (map first
       (sort (fn [file1 file2]
               (cond (requires? file1 file2)  1
                     (requires? file2 file1) -1
                     :else                    0))
             dep-tree)))

(defn warn-namespace [parsed file]
  (when-not (:namespace parsed)
    (println "Warning: Invalid or missing '-- NAMESPACE:' tag for file" file))
  parsed)

(defn topo-sort-files-by-namespace [dir-name]
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

(defn load-tables [verbose]
  (println "Loading tables...")
  (->> (map #(format "psql -h localhost -U carbon -d carbon -f %s" %)
            (topo-sort-files-by-namespace "./src/sql/tables"))
       (apply sh-wrapper "./" {:PGPASSWORD "carbon"} verbose)
       (println)))

(defn load-functions [verbose]
  (println "Loading functions...")
  (->> (map #(format "psql -h localhost -U carbon -d carbon -f %s" %)
            (topo-sort-files-by-namespace "./src/sql/functions"))
       (apply sh-wrapper "./" {:PGPASSWORD "carbon"} verbose)
       (println)))

(defn load-default-data [verbose]
  (println "Loading default data...")
  (->> (map #(format "psql -h localhost -U carbon -d carbon -f %s" %)
            (topo-sort-files-by-namespace "./src/sql/default_data"))
       (apply sh-wrapper "./" {:PGPASSWORD "carbon"} verbose)
       (println)))

(defn build-everything [verbose]
  (println "Building database...")
  (print "Please enter the postgres user's password:")
  (flush)
  (let [password (String/valueOf (.readPassword (System/console)))]
    (->> (sh-wrapper "./src/sql"
                     {:PGPASSWORD password}
                     verbose
                     "psql -h localhost -U postgres -f create_carbon_db.sql")
         (println)))
  (load-tables       verbose)
  (load-functions    verbose)
  (load-default-data verbose))

(defn -main [& args]
  (time (case (set args)
          #{"build-all"}                (build-everything false)
          #{"build-all" "verbose"}      (build-everything true)
          #{"only-functions"}           (load-functions false)
          #{"only-functions" "verbose"} (load-functions true)
          (println "Valid options are:"
                   "\n  build-all            to build the database and all components"
                   "\n  only-functions       to only build functions"
                   "\n  verbose              to show standard output from Postgres\n"))))
