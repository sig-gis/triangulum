(ns triangulum.database
  (:require [clojure.string       :as str]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :as rs]
            [triangulum.logging   :refer [log-str]]
            [triangulum.utils     :refer [format-str]]))

;;; Helper Functions

(defn- str-places
  "Creates a string with the pattern '(?, ?), (?, ?)'"
  [rows]
  (str/join ", " (repeat (count rows)
                         (str "("
                              (str/join ", " (repeat (count (first rows)) "?"))
                              ")"))))

(defn- pg-partition [rows fields]
  (partition-all (quot 32767 (count fields)) rows))

(def sql-primitive (comp val first first))

;;; Static Data

(defonce pg-db (atom {:dbtype                nil
                      :dbname                nil
                      :user                  nil
                      :password              nil
                      :reWriteBatchedInserts true}))

;; FIXME: Verify db-spec using clojure.spec
(defn set-pg-db! [db-spec]
  (swap! pg-db merge db-spec))

;;; Select Queries

(defn call-sql
  "Currently call-sql only works with postgres. The second parameter
   can be an optional settings map.

   Defaults values are:
   {:log? true :use-vec? false}"
  [sql-fn-name & opts+args]
  (let [[opts args] (if (map? (first opts+args))
                      [(first opts+args) (rest opts+args)]
                      [{} opts+args])
        {:keys [use-vec? log?] :or {use-vec? false log? true}} opts
        query           (format-str "SELECT * FROM %1(%2)"
                                    sql-fn-name
                                    (str/join "," (repeat (count args) "?")))
        query-with-args (format-str "SELECT * FROM %1(%2)"
                                    sql-fn-name
                                    (str/join "," (map pr-str args)))]
    (when log? (log-str "SQL Call: " query-with-args))
    (jdbc/execute! (jdbc/get-datasource @pg-db)
                   (into [query] (map #(condp = (type %)
                                         java.lang.Long (int %)
                                         java.lang.Double (float %)
                                         %)
                                      args))
                   {:builder-fn (if use-vec?
                                  rs/as-unqualified-lower-arrays
                                  rs/as-unqualified-lower-maps)})))

;; SQLite specific
(defn call-sqlite
  "Runs a sqllite3 sql command. An existing sqllite3 database must be provided."
  [query file-path]
  (let [db-info {:dbtype "sqlite"
                 :dbname file-path}]
    (log-str "SQLite Call: " query)
    (jdbc/execute! (jdbc/get-datasource db-info)
                   [query]
                   {:builder-fn rs/as-unqualified-lower-maps})))

;;; Insert Queries

;; TODO I dont think we need a public function for parallel and not.
(defn- for-insert-multi!
  [table cols rows]
  (into [(format-str "INSERT INTO %1 (%2) VALUES %3"
                     table
                     (str/join ", " (map name cols))
                     (str-places rows))]
        cat
        rows))

(defn insert-rows!
  "Insert new rows from 3d vector. If the optional fields are not provided,
   the first row will be assumed to be the field names."
  ([table rows]
   (insert-rows! table rows (keys (first rows))))
  ([table rows fields]
   (let [get-fields (apply juxt fields)]
     (doseq [sm-rows (pg-partition rows fields)]
       (jdbc/execute-one! (jdbc/get-datasource @pg-db)
                          (for-insert-multi! table fields (map get-fields sm-rows))
                          {})))))

(defn p-insert-rows!
  "A parallel implementation of insert-rows!"
  [table rows fields]
  (doall (pmap (fn [row-group] (insert-rows! table row-group fields))
               (pg-partition rows fields))))

;;; Update Queries

(defn- for-update-multi!
  [table cols where-col rows]
  (let [col-names  (map name cols)
        where-name (name where-col)
        set-pairs  (->> col-names
                        (remove #(= % where-name))
                        (map #(str % " = b." %))
                        (str/join ", "))
        params     (str/join ", " col-names)]
    (into [(format-str "UPDATE %1 AS t SET %2 FROM (VALUES %3) AS b (%4) WHERE t.%5 = b.%6"
                       table set-pairs (str-places rows) params where-name where-name)]
          cat
          rows)))

;; TODO I dont think we need a public function for parallel and not.
(defn update-rows!
  "Updates existing rows from a 3d vector.  One of the columns must be a
   identifier for the update command. If the optional fields are not provided,
   the first row will be assumed to be the field names."
  ([table rows id-key]
   (update-rows! table rows id-key (keys (first rows))))
  ([table rows id-key fields]
   (let [get-fields (apply juxt fields)]
     (doseq [sm-rows (pg-partition rows fields)]
       (jdbc/execute-one! (jdbc/get-datasource @pg-db)
                          (for-update-multi! table fields id-key (map get-fields sm-rows))
                          {})))))

(defn p-update-rows!
  "A parallel implementation of update-rows!"
  [table rows id-key fields]
  (doall (pmap (fn [row-group] (update-rows! table row-group id-key fields))
               (pg-partition rows fields))))
