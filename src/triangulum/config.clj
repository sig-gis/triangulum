(ns triangulum.config
  (:require [clojure.java.io    :as io]
            [clojure.edn        :as edn]
            [clojure.spec.alpha :as s]))

;;; Specs

(s/def ::host     string?)
(s/def ::port     nat-int?)
(s/def ::dbname   string?)
(s/def ::user     string?)
(s/def ::password string?)
(s/def ::domain   string?)

(s/def ::database (s/keys :req-un [::dbname ::user ::password]
                          :opt-un [::host ::port]))
(s/def ::http     (s/keys :req-un [::port]))
(s/def ::ssl      (s/keys :req-un [::domain]))

(s/def ::config (s/keys :opt-un [::database ::http ::ssl]))

;;; Private vars

(def ^:private config-file  (atom "config.edn"))
(def ^:private config-cache (atom nil))

;;; Helper Fns

(defn- read-config [new-config-file]
  (if (.exists (io/file new-config-file))
    (let [config (->> (slurp new-config-file) (edn/read-string))]
      (if (s/valid? ::config config)
        config
        (do
          (println "Error: Invalid config.edn file")
          (s/explain ::config config))))
    (println "Error: Cannot find file config.edn.")))

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
