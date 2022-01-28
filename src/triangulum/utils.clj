(ns triangulum.utils
  (:import java.io.ByteArrayOutputStream)
  (:require [cognitect.transit :as transit]
            [clojure.java.io   :as io]
            [clojure.set       :as set]
            [clojure.string    :as str]
            [clojure.data.json :as json]))

(defmacro nil-on-error
  "Uses try to catch and return nil on error"
  [& body]
  (let [_ (gensym)]
    `(try ~@body (catch Exception ~_ nil))))

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

(defn remove-end
  "Removes 'end' from string, only if it exists."
  [s end]
  (if (str/ends-with? s end)
    (subs s 0 (- (count s) (count end)))
    s))

;; Response building

(defn- clj->transit
  "Converts a clj body to transit."
  [body]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))

#_{:clj-kondo/ignore [:shadowed-var]}
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

(defn find-missing-keys
  "Returns true if m1's keys are a subset of m2's keys, and that any nested maps
   also maintain the same property.

   Example:
   `(find-missing-keys {:a {:b \"c\"} :d 0} {:a {:b \"e\" :g 42} :d 1 :h 2}) ; => true`"
  [m1 m2]
  (cond
    (and (map? m1) (map? m2))
    (let [header-diff (set/difference (-> m1 (keys) (set)) (-> m2 (keys) (set)))]
      (reduce (fn [acc [k v]]
                (into acc (find-missing-keys v (get m2 k))))
              header-diff
              m1))

    (map? m1)
    (-> m1 (keys) (set))

    :else
    #{}))

;; File operations

(defn delete-recursively
  "Recursively deletes all files in `dir`.
  (Reference)[https://gist.github.com/edw/5128978]"
  [dir]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (io/delete-file f))]
    (func func (io/file dir))))
