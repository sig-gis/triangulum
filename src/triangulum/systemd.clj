(ns triangulum.systemd
  (:require [babashka.process   :refer [process check sh pipeline pb]]
            [clojure.java.io    :as io]
            [clojure.java.shell :as sh]
            [clojure.string     :as str]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.logging :refer [log-str]]
            [triangulum.utils   :refer [parse-as-sh-cmd end-with remove-end]]))

(def ^:private path-env  (System/getenv "PATH"))
(def ^:private user-home (System/getProperty "user.home"))

(def ^:private user-systemctl    "systemctl --user ")
(def ^:private user-systemd-path (str user-home "/.config/systemd/user/multi-user.target.wants/"))

(def ^:private unit-file-template (str/trim "
[Unit]
Description=A service to launch a server written in clojure
After=network.target

[Service]
Type=notify
WorkingDirectory=%s
ExecStart=/usr/local/bin/clojure -M:server start %s %s
KillMode=process
Restart=always
PrivateTmp=true

[Install]
WantedBy=multi-user.target
"))

;; Helper functions

;; TODO consolidate sh-wrapper functions
(defn- sh-wrapper [dir env & commands]
  (io/make-parents (str dir "/dummy"))
  (sh/with-sh-dir dir
    (sh/with-sh-env (merge {:PATH path-env} env)
      (doseq [cmd commands]
        (log-str cmd)
        (let [{:keys [out err]} (apply sh/sh (parse-as-sh-cmd cmd))]
          (log-str "out: "   out)
          (log-str "error: " err))))))

(defn- sh-wrapper2 [dir env & commands]
  (io/make-parents (str dir "/dummy"))
  (doseq [cmd commands]
    (log-str cmd)
    (->
     (process (parse-as-sh-cmd cmd)
              {:dir dir
               :env (merge {:PATH path-env} env)})

     :out
     slurp
     log-str)))

(defn- enable-systemd [{:keys [repo http https dir]}]
  (let [service-name (str "cljweb-" repo)
        full-dir     (-> dir
                         (io/file)
                         (.getAbsolutePath)
                         (remove-end "."))
        repo-dir     (if (= (.getName (io/file full-dir)) repo)
                       full-dir
                       (-> full-dir
                           (end-with "/")
                           (str repo)))
        unit-file    (str user-systemd-path service-name ".service")]
    (if (.exists (io/file repo-dir "deps.edn"))
      (do
        (io/make-parents unit-file)
        (spit unit-file
              (format unit-file-template
                      repo-dir
                      (if http (str "-p " http) "")
                      (if https (str "-P " https) "")))
        (sh-wrapper "/"
                    {}
                    (str user-systemctl "daemon-reload")
                    (str user-systemctl "enable " service-name)))
      (println "The directory generated" repo-dir "does not contain a deps.edn file."))))

(defn- disable-systemd [repo]
  (let [service-name (str "cljweb-" repo)]
    (sh-wrapper "/"
                {}
                (str user-systemctl "disable " service-name)
                (str user-systemctl "daemon-reload"))
    (io/delete-file (io/file user-systemd-path (str service-name ".service")) true)))

(defn- systemctl [repo command]
  (sh-wrapper "/" {} (str user-systemctl (name command) " cljweb-" repo " --all")))

(def ^:private cli-options
  {:all       ["-a" "--all" "Starts, stops, or restarts all cljweb services when specified with the corresponding action."]
   :dir       ["-d" "--dir DIR" "Optional path to repo directory when enabling the service. Will default to the current directory."
               :default "./"]
   :http      ["-p" "--http HTTP" "Optional http port to run the server."]
   :https     ["-P" "--https HTTPS" "Optional https port to run the server."]
   :repo      ["-r" "--repo REPO" "Repository folder that contains deps.edn.  This will be used to name the service"]
   :user      ["-u" "--user USER" "The user account under which the service runs. An unprivileged user is recommended for security reasons."]})

(def ^:private cli-actions
  {:disable {:description "Disable systemd service."
             :requires    [:repo]}
   :enable  {:description "Enable systemd service. The service will be created if it doesn't exist."
             :requires    [:repo :user]}
   :restart {:description "Restart systemd service."
             :requires    [:repo]}
   :start   {:description "Start systemd service."
             :requires    [:repo]}
   :stop    {:description "Stop systemd service."
             :requires    [:repo]}})

(defn -main
  "The entry point for using the tools provided in systemd.clj."
  [& args]
  (let [{:keys [action options]} (get-cli-options args cli-options cli-actions "systemd")
        {:keys [all repo]} options]
    (and action
         (case action
           :enable  (enable-systemd options)
           :disable (disable-systemd repo)
           (systemctl (if all "*" repo) action)))
    (shutdown-agents)))
