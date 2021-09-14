(ns triangulum.type-conversion
  (:import org.postgresql.util.PGobject)
  (:require [clojure.data.json :refer [read-str write-str]]))

(defn val->int
  "Converts a value to a java Integer. Default value for failed conversion is -1."
  ([v]
   (val->int v -1))
  ([v default]
   (cond
     (instance? Integer v) v
     (number? v)           (int v)
     :else                 (try
                             (Integer/parseInt v)
                             (catch Exception _ (int default))))))

(defn val->long
  "Converts a value to a java Long. Default value for failed conversion is -1."
  ([v]
   (val->long v -1))
  ([v default]
   (cond
     (instance? Long v) v
     (number? v)       (long v)
     :else             (try
                         (Long/parseLong v)
                         (catch Exception _ (long default))))))

(defn val->float
  "Converts a value to a java Float. Default value for failed conversion is -1.0.
   Note Postgres real is equivalent to java Float."
  ([v]
   (val->float v -1.0))
  ([v default]
   (cond
     (instance? Float v) v
     (number? v)        (float v)
     :else              (try
                          (Float/parseFloat v)
                          (catch Exception _ (float default))))))

(defn val->double
  "Converts a value to a java Double. Default value for failed conversion is -1.0.
   Note Postgres float is equivalent to java Double."
  ([v]
   (val->double v -1.0))
  ([v default]
   (cond
     (instance? Double v) v
     (number? v)          (double v)
     :else                (try
                            (Double/parseDouble v)
                            (catch Exception _ (double default))))))

(defn val->bool
  "Converts a value to a java Boolean. Default value for failed conversion is false."
  ([v]
   (val->bool v false))
  ([v default]
   (if (instance? Boolean v)
     v
     (try
       (Boolean/parseBoolean v)
       (catch Exception _ (boolean default))))))

(defn- keyword-str
  "Force safe conversion of JSON keys. Invalid keywords are left as strings."
  [key-str]
  (if (and (string? key-str) (re-matches #"[\p{L}*+!_?-][\p{L}\d*+!_?-]*" key-str))
    (keyword key-str)
    key-str))

(defn json->clj
  "Convert JSON string to clj equivalent."
  ([json]
   (json->clj json nil))
  ([json default]
   (try
     (read-str json :key-fn keyword-str)
     (catch Exception _ default))))

(def jsonb->json "Convert PG jsonb object to json string." str)

(defn jsonb->clj
  "Convert PG jsonb object to clj equivalent."
  ([jsonb]
   (jsonb->clj jsonb nil))
  ([jsonb default]
   (-> jsonb jsonb->json (json->clj default))))

(def clj->json "Convert clj to JSON string." write-str)

(defn str->pg
  "Convert string to PG object of pg-type"
  [s pg-type]
  {:pre [(every? string? [s pg-type])]}
  (doto (PGobject.)
    (.setType pg-type)
    (.setValue s)))

(defn json->jsonb
  "Convert JSON string to PG jsonb object."
  [json]
  (str->pg json "jsonb"))

(defn clj->jsonb
  "Convert clj to PG jsonb object."
  [clj]
  (-> clj clj->json json->jsonb))
