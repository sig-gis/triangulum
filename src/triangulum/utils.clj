(ns triangulum.utils
  (:import java.io.ByteArrayOutputStream)
  (:require [cognitect.transit :as transit]
            [clojure.string    :as str]
            [clojure.data.json :as json]))

;; Text parsing

(defn kebab->snake
  "kebab-str -> snake_str"
  [kebab-str]
  (str/replace kebab-str "-" "_"))

(defn format-str
  "Use any char after % for format. All % are converted to %s (string)"
  [f-str & args]
  (apply format (str/replace f-str #"(%[^ ])" "%s") args))

(defn parse-as-sh-cmd
  "Split string into an array for use with clojure.java.shell/sh."
  [s]
  (loop [char-seq (seq s)
         acc      []]
    (if (empty? char-seq)
      acc
      (if (= \` (first char-seq))
        (recur (->> char-seq
                    (rest)
                    (drop-while #(not= \` %))
                    (rest))
               (->> char-seq
                    (rest)
                    (take-while #(not= \` %))
                    (apply str)
                    (str/trim)
                    (conj acc)))
        (recur (->> char-seq (drop-while #(not= \` %)))
               (->> char-seq
                    (take-while #(not= \` %))
                    (apply str)
                    (str/trim)
                    (#(str/split % #" "))
                    (remove str/blank?)
                    (into acc)))))))

(defn end-with
  "Appends 'end' to the end of the string, if it is not already the end of the string."
  [s end]
  (str s
       (when-not (str/ends-with? s end)
         end)))

;; Response building

(defn- clj->transit
  "Converts a clj body to transit."
  [body]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))

(defn data-response
  "Create a response object.
   Body is required. Status, type, and session are optional.
   When a type keyword is passed, the body is converted to that type,
   otherwise the body and type are passed through."
  ([body]
   (data-response body {}))
  ([body {:keys [status type session]
          :or   {status 200
                 type :edn}
          :as   params}]
   (merge (when (contains? params :session) {:session session})
          {:status  status
           :headers {"Content-Type" (condp = type
                                      :edn     "application/edn"
                                      :transit "application/transit+json"
                                      :json    "application/json"
                                      type)}
           :body    (condp = type
                      :edn     (pr-str         body)
                      :transit (clj->transit  body)
                      :json    (json/write-str body)
                      body)})))

;; Equivalent FP functions for maps

(defn mapm
  "Takes a map, applies f to each MapEntry, returns a map."
  [f coll]
  (persistent!
   (reduce (fn [acc cur]
             (conj! acc (f cur)))
           (transient {})
           coll)))

(defn filterm
  "Takes a map, filters on pred for each MapEntry, returns a map."
  [pred coll]
  (persistent!
   (reduce (fn [acc cur]
             (if (pred cur)
               (conj! acc cur)
               acc))
           (transient {})
           coll)))

;; Equality checking

(defn =keys
  "Whether m1 and m2 contain the same keys."
  [m1 m2]
  (and (= (keys m1) (keys m2))
       (reduce (fn [acc [k v]]
                 (and acc
                      (if (map? v)
                        (=keys v (get m2 k))
                        true)))
               true
               m1)))
