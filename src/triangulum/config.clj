(ns triangulum.config
  (:require [clojure.edn        :as edn]
            [clojure.spec.alpha :as s]
            [clojure.java.io    :as io]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.errors  :refer [nil-on-error init-throw]]))


;;; spec
;; Base spec
(s/def ::port   (s/and nat-int? #(< % 0x10000)))
(s/def ::string (s/and string? #(not (re-matches #"<.*>" %))))
(s/def ::email (s/and string? #(re-matches #"(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$" %)))

;; Mapping
(def key-mapping
  {[:database :dbname] :triangulum.database/dbname
   [:databse :user] :triangulum.database/user
   [:databse :password] :triangulum.database/password})

;; Sections
(s/def ::database (s/keys :req [:triangulum.database/dbname
                                :triangulum.database/user
                                :triangulum.database/password]
                          :opt [:triangulum.database/host
                                :triangulum.database/port]))

(s/def ::https    (s/keys :req [:triangulum.https/domain
                                :triangulum.https/email]))

(s/def ::mail     (s/keys :req [:triangulum.email/host
                                :triangulum.email/user
                                :triangulum.email/pass]
                          :opt [:triangulum.email/port]))

(s/def ::server   (s/keys :opt [:triangulum.server/mode
                                :triangulum.server/http-port
                                :triangulum.server/https-port
                                :triangulum.server/log-dir]))

(def sections [::database ::https ::server ::mail])

;; Config file
#_(s/def ::config-un (s/keys :opt-un [::database ::https ::server ::mail]))
(s/def ::config-ns (s/merge ::database ::https ::server ::mail))



;; Old Format 

(s/def ::database (s/keys :req-un [:triangulum.database/dbname
                                   :triangulum.database/user
                                   :triangulum.database/password]
                          :opt-un [:triangulum.database/host
                                   :triangulum.database/port]))

(s/def ::https    (s/keys :req-un [:triangulum.https/domain
                                   :triangulum.https/email]))

(s/def ::mail     (s/keys :req-un [:triangulum.email/host
                                   :triangulum.email/user
                                   :triangulum.email/pass]
                          :opt-un [:triangulum.email/port]))

(s/def ::server   (s/keys :opt-un [:triangulum.server/mode
                                   :triangulum.server/http-port
                                   :triangulum.server/https-port
                                   :triangulum.server/log-dir]))

;; Config file
(s/def ::config-un (s/keys :opt-un [::database ::https ::server ::mail]))

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
      (if (or (s/valid? ::config-ns config)
              (s/valid? ::config-un config))
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
