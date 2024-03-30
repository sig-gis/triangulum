(ns triangulum.systemd
  (:require [babashka.fs        :as fs]
            [clojure.java.io    :as io]
            [clojure.string     :as str]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.utils   :refer [end-with path remove-end shell-wrapper]])
  (:import com.sun.security.auth.module.UnixSystem))

(def ^:private xdg-runtime-dir    (str "/run/user/" (.getUid (UnixSystem.))))
(def ^:private shell-opts         {:dir       "/"
                                   :extra-env {"XDG_RUNTIME_DIR" xdg-runtime-dir}})
(def ^:private user-systemctl          "systemctl --user")
(def ^:private user-systemd-path       (path (fs/xdg-config-home) "systemd" "user"))
(def ^:private systemd-multi-user-path (path user-systemd-path "multi-user.target.wants"))
(def ^:private unit-file-template      (str/trim "
[Unit]
Description=A service to launch a server written in clojure
After=network.target

[Service]
Type=notify
WorkingDirectory=%s
ExecStart=/usr/local/bin/clojure -M%s:server start %s %s
KillMode=process
Restart=always
PrivateTmp=true

[Install]
WantedBy=default.target
"))

(defn fmt-service-file [{:keys [repo-dir http https extra-aliases]}]
  (format unit-file-template
          repo-dir
          (or extra-aliases "")
          (if http (str "-p " http) "")
          (if https (str "-P " https) "")))

(defn- enable-systemd [{:keys [repo dir] :as config}]
  (let [service-name (str "cljweb-" repo)
        full-dir     (-> dir
                         (fs/expand-home)
                         (fs/absolutize)
                         (remove-end "."))
        repo-dir     (if (= (fs/file-name full-dir) repo)
                       full-dir
                       (-> full-dir
                           (end-with "/")
                           (str repo)))
        service-file (str service-name ".service")
        unit-file    (path user-systemd-path service-file)]
    (if (fs/exists? (io/file repo-dir "deps.edn"))
      (do
        (fs/create-dirs user-systemd-path)
        (fs/create-dirs systemd-multi-user-path)
        (spit unit-file (fmt-service-file (merge config {:repo-dir repo-dir})))
        (fs/create-sym-link (path systemd-multi-user-path service-file) unit-file)
        (shell-wrapper shell-opts user-systemctl "daemon-reload")
        (shell-wrapper shell-opts user-systemctl "enable" service-name))
      (println "The directory generated" repo-dir "does not contain a deps.edn file."))))

(defn- disable-systemd [repo]
  (let [service-name (str "cljweb-" repo)]
    (shell-wrapper shell-opts user-systemctl "disable" service-name)
    (shell-wrapper shell-opts user-systemctl "daemon-reload")
    (io/delete-file (io/file user-systemd-path (str service-name ".service")) true)))

(defn- systemctl [repo command]
  (shell-wrapper shell-opts user-systemctl (name command) (str "cljweb-" repo) "--all"))

(def ^:private cli-options
  {:all           ["-a" "--all" "Starts, stops, or restarts all cljweb services when specified with the corresponding action."]
   :dir           ["-d" "--dir DIR" "Optional path to repo directory when enabling the service. Will default to the current directory."
                   :default "./"]
   :extra-aliases ["-A" "--extra-aliases" "Setup server to run with extra aliases from deps.edn."]
   :http          ["-p" "--http HTTP" "Optional http port to run the server."]
   :https         ["-P" "--https HTTPS" "Optional https port to run the server."]
   :repo          ["-r" "--repo REPO" "Repository folder that contains deps.edn.  This will be used to name the service"]})

(def ^:private cli-actions
  {:disable {:description "Disable systemd service."
             :requires    [:repo]}
   :enable  {:description "Enable systemd service. The service will be created if it doesn't exist."
             :requires    [:repo]}
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
