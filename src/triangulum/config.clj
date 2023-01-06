(ns triangulum.config
  (:require [clojure.java.io    :as io]
            [clojure.edn        :as edn]
            [clojure.spec.alpha :as s]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.utils   :refer [find-missing-keys]]
            [clojure.string :as str]))

;;; Specs
;; Base spec
(s/def ::port   (s/and nat-int? #(< % 0x10000)))
(s/def ::string (s/and string? #(not (re-matches #"<.*>" %))))

;; Values
(s/def ::dbname     ::string)
(s/def ::domain     ::string)
(s/def ::email      ::string) ; TODO, make an email base spec
(s/def ::host       ::string)
(s/def ::http-port  ::port)
(s/def ::https-port ::port)
(s/def ::mode       (s/and ::string #{"prod" "dev"}))
(s/def ::log-dir    ::string)
(s/def ::password   ::string)
(s/def ::pass       ::string)
(s/def ::user       ::string)

;; Sections
(s/def ::database (s/keys :req-un [::dbname ::user ::password]
                          :opt-un [::host ::port]))
(s/def ::https    (s/keys :req-un [::domain ::email]))
(s/def ::mail     (s/keys :req-un [::host ::user ::pass]
                          :opt-un [::port]))
(s/def ::server   (s/keys :opt-un [::mode ::http-port ::https-port ::log-dir]))

;; Config file
(s/def ::config (s/keys :opt-un [::database ::https ::server ::mail]))

;;; Private vars

(def ^:private ^:dynamic *default-file* "config.default.edn")

(def ^:private config-file  (atom "config.edn"))
(def ^:private config-cache (atom nil))

;;; Helper Fns

(defn- wrap-throw [& strs]
  (-> (apply str strs)
      (ex-info {})
      (throw)))

(defn- read-config [file]
  (if (.exists (io/file file))
    (let [example-config (-> (slurp *default-file*) (edn/read-string))
          config         (-> (slurp file) (edn/read-string))
          missing-keys   (find-missing-keys example-config config)]
      (cond
        (seq missing-keys)
        (wrap-throw "Error: The following keys from config.default.edn are missing from: "
                    file
                    "\n"
                    (str/join "', '" missing-keys))

        (not (s/valid? ::config config))
        (do (println "Error: Invalid config file: " file)
            (s/explain ::config config)
            (flush)
            (wrap-throw ""))

        :else
        config))
    (wrap-throw "Error: Cannot find file " file)))

(defn- cache-config []
  (or @config-cache
      (reset! config-cache (read-config @config-file))))

;;; Public Fns

(defn load-config
  "Re/loads a configuration file. Defaults to the last loaded file, or config.edn."
  ([]
   (load-config @config-file))
  ([new-config-file]
   (reset! config-file new-config-file)
   (reset! config-cache (read-config @config-file))))

(defn get-config
  "Retrieves the key `k` from the config file.
   Can also be called with the keys leading to a config.
   Examples:
   ```clojure
   (get-config :mail) -> {:host \"google.com\" :port 543}
   (get-config :mail :host) -> \"google.com\"
   ```"
  [& all-keys]
  (get-in (cache-config) all-keys))

(defn valid-config?
  "Validates `file` as a configuration file."
  [{:keys [file] :or {file @config-file}}]
  (map? (read-config file)))

(def ^:private cli-options
  {:file ["-f" "--file FILE" "Configuration file to validate."]})

(def ^:private cli-actions
  {:validate {:description "Validates the configuration file (default: config.edn)."
              :requires    []}})

(defn -main
  "Configuration management."
  [& args]
  (let [{:keys [action options]} (get-cli-options args cli-options cli-actions "config")]
    (case action
      :validate (valid-config? options)
      nil))
  (shutdown-agents))
