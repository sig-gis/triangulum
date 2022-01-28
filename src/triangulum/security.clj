(ns triangulum.security
  (:require [clojure.java.io :as io])
  (:import [java.security MessageDigest]))

;; https://gist.github.com/kisom/1698245
(defn hash-str
  "Returns the hex digest of string. Defaults to SHA-256 hash."
  ([input] (hash-str input "SHA-256"))
  ([^java.lang.String input hash-algo]
     (if (string? input)
       (let [md (MessageDigest/getInstance hash-algo)]
         (.update md (.getBytes input))
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
        (hash-str))))
