(ns triangulum.build-db
  (:import [java.io File]
           [java.security MessageDigest])
  (:require [clojure.java.io    :as io]
            [clojure.edn        :as edn]))

;; https://gist.github.com/kisom/1698245
(defn hexdigest
  "Returns the hex digest of an object. Expects string as input.
   Defaults to SHA-256 hash."
  ([input] (hexdigest input "SHA-256"))
  ([input hash-algo]
     (if (string? input)
       (let [md (MessageDigest/getInstance hash-algo)]
         (. md update (.getBytes input))
         (let [digest (.digest md)]
           (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))
       (do
         (println "Invalid input to hexdigst! Expected string, got" (type input))
         nil))))

(defn hash-file
  "Returns the sha256 digest of a file."
  [filename]
  (when (.isFile (io/file filename))
    (-> filename
        (slurp)
        (hexdigest))))

(defn compare-sha256
  "Compare an object to a hash; true if (= (hash obj) ref-hash)."
  [obj ref-hash]
  (= ref-hash (hexdigest obj "SHA-256")))
