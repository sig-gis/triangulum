(ns triangulum.utils
  (:require [babashka.process   :refer [shell]]
            [clojure.data.json  :as json]
            [clojure.set        :as set]
            [clojure.string     :as str]
            [cognitect.transit  :as transit]
            [triangulum.logging :refer [log-str]])
  (:import java.io.ByteArrayOutputStream))

;;; Text parsing

(defn kebab->snake
  "Converts kebab-str to snake_str."
  [kebab-str]
  (str/replace kebab-str "-" "_"))

(defn kebab->camel
  "Converts kebab-string to camelString."
  [kebab-string]
  (let [words (-> kebab-string
                  (str/lower-case)
                  (str/split #"-"))]
    (->> (map str/capitalize (rest words))
         (cons (first words))
         (str/join ""))))

(defn camel->kebab
  "Converts camelString to kebab-string."
  [camel-string]
  (as-> camel-string text
    (str/split text #"(?<=[a-z])(?=[A-Z])")
    (map str/lower-case text)
    (str/join "-" text)))

(defn format-str
  "Use any char after % for format. All % are converted to %s (string)."
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

;;; Shell commands

(defn shell-wrapper
  "A wrapper around babashka.process/shell that logs the output and errors.
  Accepts an optional opts map as the first argument, followed by the command and its arguments.
  The :log key in the opts map can be used to control logging (default is true).

  Usage:
  (shell-wrapper {} \"ls\" \"-l\") ; With an opts map
  (shell-wrapper \"ls\" \"-l\") ; Without an opts map
  (shell-wrapper {:log false} \"ls\" \"-l\") ; Disabling logging

  Examples:
  1. Logs the output and errors by default:
  (shell-wrapper {} \"ls\" \"-l\")

  2. Can be called without an opts map, assuming default values:
  (shell-wrapper \"ls\" \"-l\")

  3. Disabling logging using the :log? key in the opts map:
  (shell-wrapper {:log false} \"ls\" \"-l\")"
  [& args]
  (let [opts   (if (map? (first args)) (first args) {})
        cmd    (if (map? (first args)) (rest args) args)
        log?   (get opts :log true)
        result (apply shell
                      (merge opts
                             {:continue true
                              :out      :string
                              :err      :string})
                      cmd)]
    (when log?
      (log-str "cmd: " (str/join " " (:cmd result)))
      (some-> (:out result) not-empty (log-str "out: "))
      (some-> (:err result) not-empty (log-str "error: ")))
    result))

(defn ^:deprecated sh-wrapper
  "DEPRECATED: Use [[triangulum.utils/shell-wrapper]] instead.
  Takes a directory, an environment, a verbosity flag, and bash commands.
  Executes the commands using the given path and environment, then returns
  the output (errors by default)."
  [dir env verbose & commands]
  (reduce (fn [acc cmd]
            (let [{:keys [out err]} (shell-wrapper
                                     {:dir       dir
                                      :extra-env env
                                      :log       false}
                                     cmd)]
              (str acc (when verbose out) err)))
          ""
          commands))

(defn sh-exec-with
  "Provides a path (`dir`) and environment (`env`) to one bash `command`
   and executes it. Returns a map in the following format:
   `{:exit 0 :out 'Output message\n' :err ''}`"
  [dir env command]
  (-> (shell-wrapper {:dir dir
                      :extra-env env
                      :log false}
                     command)
      (select-keys [:exit :out :err])))

;;; Response building

(defn- clj->transit
  "Converts a clj body to transit."
  [body]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))

#_{:clj-kondo/ignore [:shadowed-var]}
(defn ^:deprecated data-response
  "DEPRECATED: Use [[triangulum.response/data-response]] instead.
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

(defn reverse-map
  "Reverses the key-value pairs in a given map."
  [m]
  (zipmap (vals m) (keys m)))

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
