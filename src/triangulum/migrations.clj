(ns triangulum.migrations
  (:import [java.io File])
  (:require [clojure.java.io     :as io]
            [clojure.edn         :as edn]
            [clojure.set         :refer [difference]]
            [clojure.java.shell  :as sh]
            [clojure.string      :as str]
            [next.jdbc           :as jdbc]
            [triangulum.cli      :refer [get-cli-options]]
            [triangulum.config   :refer [get-config]]
            [triangulum.security :refer [hexdigest compare-sha256]]
            [triangulum.utils    :refer [nil-on-error parse-as-sh-cmd format-str]]))


;; Constants
(def ^:private migrations-dir    "./src/sql/changes")
(def ^:private migrations-schema "triangulum")
(def ^:private migrations-table  "triangulum.migrations")

;; Helper Fns

(defn- get-ds [database user user-pass]
  (jdbc/get-datasource {:dbtype                "postgresql"
                        :dbname                database
                        :user                  user
                        :password              user-pass
                        :reWriteBatchedInserts true}))

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

(defn- get-completed-changes [ds]


  (let [file (io/file change-file)]
    (if (.exists file)
      (-> file (slurp) (edn/read-string))
      #{})))

(defn- set-completed-changes [new-changes]
  (spit change-file (prn-str new-changes)))

(defn- check-conn [database user user-pass]
  (jdbc/get-connection (get-ds database user user-pass)))

(defn- apply-change-sql-file [database user user-pass verbose? file]
  (when verbose? (println (format "Migrating change %s " file)))
  (let [ds (get-ds database user user-pass)]
    (jdbc/with-transaction [tx ds]
      (jdbc/execute! tx [(slurp (str migrations-dir "/" file))]))))
