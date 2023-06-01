(ns triangulum.security
  (:import java.security.MessageDigest))

;; https://gist.github.com/kisom/1698245
(defn hash-digest
  "Returns the hex digest of string. Defaults to SHA-256 hash."
  ([^String input] (hash-digest input "SHA-256"))
  ([^String input hash-algorithm]
   (let [string-as-bytes (.getBytes input)
         message-digest  (doto (MessageDigest/getInstance hash-algorithm)
                           (.update string-as-bytes))]
     (->> (.digest message-digest)
          (map #(format "%02x" (bit-and % 0xff)))
          (apply str)))))

(defn hash-file
  "Returns the SHA-256 digest of a file."
  [filename]
  (-> filename
      (slurp)
      (hash-digest)))
