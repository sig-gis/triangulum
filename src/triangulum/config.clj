(ns triangulum.config
  (:require [clojure.edn                       :as edn]
            [clojure.java.io                   :as io]
            [clojure.spec.alpha                :as s]
            [clojure.string                    :as str]
            [triangulum.cli                    :refer [get-cli-options]]
            [triangulum.config-nested-spec     :as config-nested]
            [triangulum.config-namespaced-spec :as config-namespaced]
            [triangulum.errors                 :refer [nil-on-error init-throw]]
            [triangulum.utils                  :refer [reverse-map]]))

;;; spec

;; Base spec
(s/def ::port   (s/and nat-int? #(< % 0x10000)))
(s/def ::string (s/and string? #(not (re-matches #"<.*>" %))))
(s/def ::email  (s/and string? #(re-matches #"(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$" %)))
(s/def ::namespaced-symbol (s/and symbol? #(namespace %)))
(s/def ::url (s/and string? #(re-matches #"^https?://.+" %)))
(s/def ::static-file-path (s/and string? #(re-matches #"/[^:*?\"<>|]*" %)))
(s/def ::path (s/and string? #(re-matches #"[./][^:*?\"<>|]*" %)))
(s/def ::hostname (s/and string? #(re-matches #"[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" %)))


;; Config file

(s/def ::nested-config (s/keys :opt-un [::config-nested/server
                                        ::config-nested/app
                                        ::config-nested/database
                                        ::config-nested/mail
                                        ::config-nested/https]))

(s/def ::namespaced-config (s/merge ::config-namespaced/server
                                    ::config-namespaced/app
                                    ::config-namespaced/database
                                    ::config-namespaced/mail
                                    ::config-namespaced/https))

;;; Private vars

(def ^:private config-file  (atom "config.edn"))
(def ^:private config-cache (atom nil))
(def ^:private ns->un-mapping
  "Maps namespaced keys namesapces to their unnamespaced counterparts."
  {:views :app, :email :mail})
(def ^:private un->ns-mapping
  "Maps unnamespaced keys namespaces to their namespaced counterparts."
  (reverse-map ns->un-mapping))


;;; Helper Fns

(defn- wrap-throw [& strs]
  (-> (apply str strs)
      (init-throw)))

(defn- namespaced-key?
  "Returns true if the given key has a namespace, otherwise false."
  [k]
  (namespace k))

(defn- read-config [file]
  (if (.exists (io/file file))
    (if-let [config (nil-on-error (edn/read-string (slurp file)))]
      (let [valid-nested-config? (s/valid? ::nested-config config)
            valid-namespaced-config? (s/valid? ::namespaced-config config)]
        (cond (or valid-nested-config?
                  valid-namespaced-config?)
              config

              (every? namespaced-key? (keys config))
              (wrap-throw "Error: Config file " file " failed spec check:\n" (s/explain-str ::namespaced-config config))

              :else
              (wrap-throw "Error: Config file " file " failed spec check:\n" (s/explain-str ::nested-config config))))
      (wrap-throw "Error: Config file " file " does not contain valid EDN."))
    (wrap-throw "Error: Cannot find config file " file ".")))

(defn- cache-config []
  (or @config-cache
      (reset! config-cache (read-config @config-file))))

(defn- get-mapped-key-ns
  "Given a namespaced key, returns the corresponding unnamespaced key."
  [ns-key]
  (let [new-ns (-> ns-key
                   namespace
                   (clojure.string/split #"\.")
                   second
                   keyword)]
    (get ns->un-mapping new-ns new-ns)))


;;; Public Fns

(defn load-config
  "Re/loads a configuration file. Defaults to the last loaded file, or config.edn."
  ([]
   (load-config @config-file))
  ([new-config-file]
   (reset! config-file new-config-file)
   (reset! config-cache (read-config @config-file))))

(defn namespaced-config?
  "Returns true if the given configuration map is namespaced, otherwise false."
  [config]
  (s/valid? ::namespaced-config config))

(defn nested-config?
  [config]
  (not (namespaced-config? config)))

(defn split-ns-key
  "Given a namespaced key, returns a vector of unnamespaced keys."
  [ns-key]
  [(get-mapped-key-ns ns-key) (-> ns-key (name) (keyword))])

(defn join-un-key
  "Given a sequence of unnamespaced keys, returns a single namespaced key."
  ([key-path] (join-un-key key-path {:prefix "triangulum"}))
  ([key-path {:keys [prefix]}]
   (let [[n k] key-path
         n (get un->ns-mapping n n)]
     (keyword (str prefix "." (name n)) (name k)))))

;; Retrieves a configuration value for the given key(s).
(defn get-config
  "Retrieves the key `k` from the config file.
   Can also be called with the keys leading to a config.
   Examples:
   ```clojure
   (get-config :mail) -> {:host \"google.com\" :port 543}
   (get-config :mail :host) -> \"google.com\"
   (get-config :triangulum.email/host) -> \"google.com\" 
   (get-config :triangulum.views/title :en)
   ```"
  [& all-keys]
  (let [config (cache-config)
        k (first all-keys)]
    (cond
      (= (count all-keys) 1)
      (cond
        (and (nested-config? config)
             (namespaced-key? k))
        (get-in config (split-ns-key k))

        (and (namespaced-config? config)
             (not (namespaced-key? k)))
        (->> config
             (filter #(= (get-mapped-key-ns (key %)) k))
             (into {}))

        :else
        (get config k))

      (and (= (count all-keys) 2)
           (namespaced-config? config)
           (not (namespaced-key? (first all-keys))))
      (get config (join-un-key all-keys))

      :else
      (get-in config all-keys))))

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
