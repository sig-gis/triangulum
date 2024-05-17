(ns triangulum.database
  "To use `triangulum.database`, first add your database connection
  configurations to a `config.edn` file in your project's root directory.

  For example:
  ```clojure
  ;; config.edn
  {:database {:host     \"localhost\"
            :port     5432
            :dbname   \"pyregence\"
            :user     \"pyregence\"
            :password \"pyregence\"}}
  ```

  To run a postgres sql command use `call-sql`. Currently `call-sql`
  only works with postgres. The second parameter can be an optional
  settings map (default values shown below).

  ```clojure
  (call-sql \"function\" {:log? true :use-vec? false} \"param1\" \"param2\" ... \"paramN\")
  ```

  To run a sqllite3 sql command use `call-sqlite`. An existing sqllite3 database
  must be provided.

  ```clojure
  (call-sqlite \"select * from table\" \"path/db-file\")
  ```

  To insert new rows or update existing rows, use `insert-rows!` and
  `update-rows!`. If fields are not provided, the first row will be
  assumed to be the field names.

  ```clojure
  (insert-rows! table-name rows-vector fields-map)
  (update-rows! table-name rows-vector column-to-update fields-map)
  ```"
  (:import java.sql.Array
           (java.sql PreparedStatement)
           (org.postgresql.util PGobject))
  (:require [clojure.spec.alpha   :as s]
            [clojure.data.json    :as json]
            [clojure.string       :as str]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.prepare    :as prepare]
            [triangulum.config    :as config :refer [get-config]]
            [triangulum.logging   :refer [log-str]]
            [triangulum.utils     :refer [format-str]]))

;; spec

(s/def ::dbname   ::config/string)
(s/def ::user     ::config/string)
(s/def ::password ::config/string)
(s/def ::host     ::config/string)
(s/def ::port     ::config/port)

;; JSON encoding/decoding helpers

(def ->json json/write-str)
(def <-json #(json/read-str % :key-fn keyword))

;; Functions for converting between Clojure data and PGobject

(defn ->pgobject
  "Convert Clojure data to a PGobject with JSON content.
  The default type is `jsonb`, but can be overridden with :pgtype metadata."
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Convert PGobject containing `json` or `jsonb` to Clojure data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (<-json value) {:pgtype type}))
      value)))

(extend-protocol prepare/SettableParameter
  "Convert Clojure maps and vectors to PGobject for JSON/JSONB."
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

(extend-protocol rs/ReadableColumn
  ;; Convert SQL arrays to Clojure vectors.
  Array
  (read-column-by-label [^Array v _]    (vec (.getArray v)))
  (read-column-by-index [^Array v _ _]  (vec (.getArray v)))

  ;; Convert PGobject columns to Clojure data if type is `json` or `jsonb`.
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))

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

(def sql-primitive
  "Return single value for queries that return a value instead of a table."
  (comp val first first))

;;; Static Data

(defn- pg-db []
  (merge {:dbtype                "postgresql"
          :reWriteBatchedInserts true}
         (get-config :database)))

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
    (jdbc/execute! (jdbc/get-datasource (pg-db))
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
       (jdbc/execute-one! (jdbc/get-datasource (pg-db))
                          (for-insert-multi! table fields (map get-fields sm-rows))
                          {})))))

(defn p-insert-rows!
  "A parallel implementation of insert-rows!"
  ([table rows]
   (p-insert-rows! table rows (keys (first rows))))
  ([table rows fields]
   (doall (pmap (fn [row-group] (insert-rows! table row-group fields))
                (pg-partition rows fields)))))

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
       (jdbc/execute-one! (jdbc/get-datasource (pg-db))
                          (for-update-multi! table fields id-key (map get-fields sm-rows))
                          {})))))

(defn p-update-rows!
  "A parallel implementation of update-rows!"
  ([table rows id-key]
   (p-update-rows! table rows id-key (keys (first rows))))
  ([table rows id-key fields]
   (doall (pmap (fn [row-group] (update-rows! table row-group id-key fields))
                (pg-partition rows fields)))))
