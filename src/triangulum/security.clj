(ns triangulum.security
  (:import java.security.MessageDigest))

;; https://gist.github.com/kisom/1698245
(defn hash-digest
  "Returns the hex digest of string. Defaults to SHA-256 hash."
  ([input] (hash-digest input "SHA-256"))
  ([input hash-algo]
   (let [md (doto (MessageDigest/getInstance hash-algo)
              (.update (.getBytes ^String input)))]
     (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest md))))))

(defn hash-file
  "Returns the sha256 digest of a file."
  [filename]
  (-> filename
      (slurp)
      (hash-digest)))
