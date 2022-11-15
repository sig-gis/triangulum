(ns triangulum.utils
  (:import java.io.ByteArrayOutputStream)
  (:require [clojure.data.json   :as json]
            [clojure.java.shell  :as sh]
            [clojure.set         :as set]
            [clojure.string      :as str]
            [cognitect.transit   :as transit]))

;; Text parsing

(defn kebab->snake
  "kebab-str -> snake_str"
  [kebab-str]
  (str/replace kebab-str "-" "_"))

(defn kebab->camel
  "kebab-str -> camelCaseStr"
  [k]
  (let [words (str/split (name k) #"-")]
    (->> (map str/capitalize (rest words))
         (apply str (first words)))))

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
                    (str/blank?)
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

;; Shell commands

(def ^:private path-env (System/getenv "PATH"))

(defn sh-wrapper
  "Provides a given path and environment to a set of bash commands,
  and parses the output, creating an array as described in `parse-as-sh-cmd`."
  [dir env verbose & commands]
  (sh/with-sh-dir dir
    (sh/with-sh-env (merge {:PATH path-env} env)
      (reduce (fn [acc cmd]
                (let [{:keys [out err]} (apply sh/sh (parse-as-sh-cmd cmd))]
                  (str acc (when verbose out) err)))
              ""
              commands))))

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
  "DEPRECATED: Use 'triangulum.response/data-response' instead.
   Create a response object.
   Body is required. Status, type, and session are optional.
   When a type keyword is passed, the body is converted to that type,
   otherwise the body is converted to edn."
  ([body]
   (data-response body {}))
  ([body {:keys [status type session]
          :or   {status 200
                 type   :edn}
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
  "Returnss true if m1's keys are a subset of m2's keys, and that any nested maps
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

;; Namespace operations

(defn resolve-foreign-symbol
  "Given a namespace-qualified symbol, attempt to require its namespace
  and resolve the symbol within that namespace to a value."
  [sym]
  (require (symbol (namespace sym)))
  (resolve sym))
