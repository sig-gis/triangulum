(ns triangulum.utils
  (:import java.io.ByteArrayOutputStream)
  (:require [cognitect.transit :as transit]
            [clojure.string :as str]
            [clojure.data.json :as json]))

;; Text parsing

(defn kebab->snake [kebab-str]
  (str/replace kebab-str "-" "_"))

(defn format-%
  "Use any char after % for format."
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

;; Response building

(defn body->transit [body]
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
  ([body {:keys [status c-type session]
          :or   {status 200
                 c-type :edn}
          :as   params}]
   (merge (when (contains? params :session) {:session session})
          {:status  status
           :headers {"Content-Type" (condp = c-type
                                      :edn     "application/edn"
                                      :transit "application/transit+json"
                                      :json    "application/json"
                                      c-type)}
           :body    (condp = c-type
                      :edn     (pr-str         body)
                      :transit (body->transit  body)
                      :json    (json/write-str body)
                      body)})))

;; Equivalent FP functions for maps

(defn mapm [f coll]
  (persistent!
   (reduce (fn [acc cur]
             (conj! acc (f cur)))
           (transient {})
           coll)))

(defn filterm [pred coll]
  (persistent!
   (reduce (fn [acc cur]
             (if (pred cur)
               (conj! acc cur)
               acc))
           (transient {})
           coll)))
