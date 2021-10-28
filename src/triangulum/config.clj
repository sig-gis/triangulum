(ns triangulum.config
  (:require [clojure.java.io    :as io]
            [clojure.edn        :as edn]
            [clojure.spec.alpha :as s]
            [triangulum.cli     :refer [get-cli-options]]))

;;; Specs

(s/def ::host       string?)
(s/def ::port       (s/and nat-int? #(< % 0x10000)))
(s/def ::https-port (s/and nat-int? #(< % 0x10000)))
(s/def ::dbname     string?)
(s/def ::user       string?)
(s/def ::password   string?)
(s/def ::domain     string?)
(s/def ::mode       (s/and string? #{"prod" "dev"}))
(s/def ::output-dir string?)

(s/def ::database (s/keys :req-un [::dbname ::user ::password]
                          :opt-un [::host ::port]))
(s/def ::http     (s/keys :req-un [::port]))
(s/def ::ssl      (s/keys :req-un [::domain]))
(s/def ::server   (s/keys :req-un [::mode ::port]
                          :opt-un [::https-port ::output-dir]))

(s/def ::config (s/keys :opt-un [::database ::http ::ssl ::server]))

;;; Private vars

(def ^:private config-file  (atom "config.edn"))
(def ^:private config-cache (atom nil))

;;; Helper Fns

(defn- read-config [file]
  (if (.exists (io/file file))
    (let [config (->> (slurp file) (edn/read-string))]
      (if (s/valid? ::config config)
        config
        (do
          (println "Invalid config file:" file)
          (s/explain ::config config))))
    (println "Error: Cannot find file" file)))

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
   (reset! config-cache nil)))

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
  {:validate ["-f" "--file FILE" "Configuration file to validate."]})

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
