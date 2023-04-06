(ns triangulum.config
  (:require [clojure.edn        :as edn]
            [clojure.spec.alpha :as s]
            [clojure.java.io    :as io]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.errors  :refer [nil-on-error init-throw]]))

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

;; Private vars

(def ^:private config-file  (atom "config.edn"))
(def ^:private config-cache (atom nil))

;;; Helper Fns

(defn- wrap-throw [& strs]
  (-> (apply str strs)
      (init-throw)))

(defn- read-config [file]
  (if (.exists (io/file file))
    (if-let [config (nil-on-error (edn/read-string (slurp file)))]
      (if (s/valid? ::config config)
        config
        (wrap-throw "Error: Config file " file " failed spec check:\n" (s/explain-str ::config config)))
      (wrap-throw "Error: Config file " file " does not contain valid EDN."))
    (wrap-throw "Error: Cannot find config file " file ".")))

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
