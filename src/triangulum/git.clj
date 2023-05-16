(ns triangulum.git
  (:require [clojure.string     :as str]
            [clojure.data.json  :as json]
            [clojure.spec.alpha :as s]
            [clj-http.client    :as client]
            [triangulum.config  :as config :refer [get-config]]
            [triangulum.logging :refer [log-str]]))

;; spec

(s/def ::tags-url ::config/url)

;; Constants

(def tags-url
  "Gets repo tags url from config.edn."
  (get-config :app :tags-url))

;; Cache

(def ^:private version (atom :undefined))

;; Private Fns

(defn- get-all-tags []
  (try
    (let [{:keys [status body]} (client/get tags-url)]
      (when (= 200 status)
        (json/read-str body :key-fn keyword)))
    (catch Exception e (log-str e))))

(defn- latest-prod-tag []
  (some->> (get-all-tags)
           (map :name)
           (filter #(str/starts-with? %1 "prod"))
           (sort)
           (last)))

;; Public Fns

(defn current-version
  "Return current latest tag version from the configured tags url of repo."
  []
  (cond
    (nil? tags-url)         nil
    (nil? @version)         nil
    (= @version :undefined) (reset! version (latest-prod-tag))
    :else                   @version))
