(ns triangulum.config
  (:require [clojure.edn        :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string     :as str]
            [clojure.java.io    :as io]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.errors  :refer [nil-on-error]]
            [triangulum.utils   :refer [find-missing-keys]]))

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

;; FIXME: Fill in the rest of the triangulum map keys and their
;; corresponding lookup expressions. Then make sure to merge in all of
;; the remaining app keys in the nested-config.
(defn- transform-nested-config-to-namespaced-config [nested-config]
  (->>
   {:triangulum.git/tags-url     (get-in nested-config [:app :tags-url])
    :triangulum.fill/the-rest-in (get-in nested-config [:other :paths])}
   ;; Remember to dissoc all of the transformed keys above from the
   ;; nested map and then merge the remainder (app-specific keys) into
   ;; this new config map before returning it.
   (remove (fn [[_k v]] (nil? v)))
   (into {})))

(defn- wrap-throw [& strs]
  (-> (apply str strs)
      (ex-info {})
      (throw)))

(defn- first-failed-spec [edn specs]
  (->> specs
       (filter (fn [spec] (not (s/valid? spec edn))))
       (first)))

(defn- validate-edn [edn specs]
  (if-let [failed-spec (first-failed-spec edn specs)]
    {:valid? false
     :result (s/explain-str failed-spec edn)}
    {:valid? true
     :result edn}))

(defn- get-nested-specs [edn]
  (let [triangulum-spec ::nested-config ; FIXME: define this spec
        app-spec        (get-in edn [:app :config-spec])] ; FIXME: define this spec and load this namespace on demand
    (remove nil? [triangulum-spec app-spec])))

(defn- get-namespaced-specs [edn]
  (let [triangulum-spec ::namespaced-config ; FIXME: define this spec
        app-spec        (get edn ::app-spec)] ; FIXME: define this spec and load this namespace on demand
    (remove nil? [triangulum-spec app-spec])))

(defn- read-edn-from-file [filename]
  (let [file (io/file filename)]
    (if-not (.exists file)
      (wrap-throw "Error: Cannot find " file ".")
      (if-not (.canRead file)
        (wrap-throw "Error: Config file " file " is not readable.")
        (if-let [edn (-> (slurp file)
                         (edn/read-string)
                         (nil-on-error))]
          edn
          (wrap-throw "Error: Config file " file " does not contain well-formed EDN."))))))

(defn- read-typed-config [file]
  (let [edn                   (read-edn-from-file file)
        nested-specs          (get-nested-specs edn)
        namespaced-specs      (get-namespaced-specs edn)
        nested-validation     (validate-edn edn nested-specs)
        namespaced-validation (validate-edn edn namespaced-specs)]
    (cond
      (:valid? nested-validation)
      {:spec-type :nested
       :edn       (:result nested-validation)}

      (:valid? namespaced-validation)
      {:spec-type :namespaced
       :edn       (:result namespaced-validation)}

      :else
      (wrap-throw "Error: Config file " file " does not conform to either the nested or namespaced spec.\n\n"
                  "Spec failure for nested spec:\n" (:result nested-validation) "\n\n"
                  "Spec failure for namespaced spec:\n" (:result namespaced-validation)))))

(defn- read-config [file]
  (let [example-config (read-typed-config *default-file*)
        user-config    (read-typed-config file)]
    (if (not= (:spec-type example-config)
              (:spec-type user-config))
      (wrap-throw "The config files " *default-file* " and " file " do not conform to the same spec.")
      (if (= :namespaced (:spec-type user-config))
        (:edn user-config)
        (transform-nested-config-to-namespaced-config (:edn user-config))))))

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
