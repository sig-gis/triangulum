(ns triangulum.systemd
  (:require [clojure.java.io    :as io]
            [clojure.java.shell :as sh]
            [clojure.string     :as str]
            [triangulum.cli     :refer [get-cli-options]]
            [triangulum.logging :refer [log-str]]
            [triangulum.utils   :refer [parse-as-sh-cmd]]))

(def ^:private path-env (System/getenv "PATH"))

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

(def ^:private unit-file-template (str/trim "
[Unit]
Description=A service to launch a server written in clojure
After=network.target

[Service]
Type=simple
User=%s
WorkingDirectory=%s
ExecStart=/usr/local/bin/clojure -M:run-server %s %s -o logs

[Install]
WantedBy=multi-user.target
"))

(defn- enable-systemd [{:keys [repo user http https]}]
  (if (and repo
           (.exists (io/file repo "deps.edn")))
    (let [full-path (.getAbsolutePath (io/file repo))
          service-name (as-> full-path %
                         (str/split % #"/")
                         (filter #(re-matches #"\w*" %) %)
                         (last %)
                         (str "cljweb-" %))]
      (spit (io/file "/etc/systemd/system/" (str service-name ".service"))
            (format unit-file-template
                    user
                    (.getAbsolutePath (io/file repo))
                    (if http (str "-p " http) "")
                    (if https (str "-P " https) "")))
      (sh-wrapper "/"
                  {}
                  "systemctl daemon-reload"
                  (str "systemctl enable " service-name)))
    (println "A repository directory containing deps.edn must be supplied.")))

(defn- disable-systemd [repo]
  (if (nil? repo)
    (println "You must specify a repo with -r when disabling the systemd unit file.")
    (let [service-name (str "cljweb-" repo)]
      (sh-wrapper "/"
                  {}
                  (str "systemctl disable " service-name)
                  "systemctl daemon-reload")
      (io/delete-file (io/file "/etc/systemd/system/" (str service-name ".service")) true))))

(defn- systemctl [repo command]
  (sh-wrapper "/" {} (str "systemctl " (name command) " cljweb-" repo " --all")))

(def ^:private cli-options
  {:all      ["-a" "--all" "Starts, stops, or restarts all cljweb services when specified with the corresponding action."]
   :no-http  ["-p" "--http HTTP" "Optional http port to run the server."]
   :no-https ["-P" "--https HTTPS" "Optional https port to run the server."]
   :repo     ["-r" "--repo REPO" "Repository folder that contains deps.edn.  With enable, this must be a complete path."]
   :user     ["-u" "--user USER" "The user account under which the service runs. An unprivileged user is recommended for security reasons."]})

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
           (systemctl (if all "*" repo) action))))
  (shutdown-agents))
