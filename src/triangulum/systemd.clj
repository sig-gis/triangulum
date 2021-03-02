(ns triangulum.systemd
  (:require [clojure.java.io    :as io]
            [clojure.java.shell :as sh]
            [clojure.tools.cli  :refer [parse-opts]]
            [triangulum.logging :refer [log-str]]
            [triangulum.utils   :refer [parse-as-sh-cmd]]))

(def path-env (System/getenv "PATH"))

;; TODO consolidate sh-wrapper functions
(defn sh-wrapper [dir env & commands]
  (io/make-parents (str dir "/dummy"))
  (sh/with-sh-dir dir
    (sh/with-sh-env (merge {:PATH path-env} env)
      (doseq [cmd commands]
        (log-str cmd)
        (let [{:keys [out err]} (apply sh/sh (parse-as-sh-cmd cmd))]
          (log-str "out: "   out)
          (log-str "error: " err)
          (= err ""))))))

(defn enable-systemd [repo path user offset]
  (cond
    (nil? user)
    (println "You must specify a user with -u when enabling the systemd unit file.")

    (nil? repo)
    (println "You must specify a repo with -r when enabling the systemd unit file.")

    :else
    (let [unit-file (str "cljweb-" repo)]
      (spit (io/file "/etc/systemd/system/" (str unit-file ".service"))
            (format (str "[Unit]\n"
                         "Description=A service to launch a server written in clojure\n"
                         "After=network.target\n\n"
                         "[Service]\n"
                         "Type=simple\n"
                         "User=%s\n"
                         "WorkingDirectory=%s\n"
                         "ExecStart=/usr/local/bin/clojure -M:run-server -p %s -P %s -o logs\n"
                         "[Install]\n\n"
                         "WantedBy=multi-user.target\n")
                    user
                    (.getAbsolutePath (io/file path repo))
                    (+ offset 8080)
                    (+ offset 8443)))
      (sh-wrapper "/"
                  {}
                  "systemctl daemon-reload"
                  (str "systemctl enable " unit-file)
                  (str "systemctl restart " unit-file)))))

(defn disable-systemd [repo]
  (if repo
    (sh-wrapper "/" {} (str "systemctl disable cljweb-" repo))
    (println "You must specify a repo with -r when disabling the systemd unit file.")))

(defn systemctl [repo command]
  (sh-wrapper "/" {} (str "systemctl " command " cljweb-" repo " --all")))

(def cli-options
  [["-a" "--all" "Disables, starts, or stops all cljweb services when specified with the corresponding action."]
   ["-D" "--disable" "Disable systemd service."]
   ["-E" "--enable" "Enable systemd service. The service will be created if it doesn't exist."]
   ["-o" "--offset OFFSET" "Numerical offset from the standard ports of 8080 and 8443."
    :default 0
    :parse-fn #(if (int? %) % (Integer/parseInt %))
    :validate [#(< 0 % 100) "Must be a number between 0 and 100."]]
   ["-p" "--path PATH" "Alternative path for git repo location."
    :default "/sig"]
   ["-r" "--repo REPO" "Repository folder name in /sig or path specified with -p."]
   ["-S" "--start" "Start systemd service."]
   ["-T" "--stop" "Stop systemd service."]
   ["-u" "--user USER" "The user which to run the server as. Require when -E is specified."]])

(defn -main [& args]
  (let [{:keys [options summary errors]} (parse-opts args cli-options)
        {:keys [all disable enable repo offset path restart start stop user]} options
        command (or (and restart "restart")
                    (and start "start")
                    (and stop "stop"))]
    (cond
      (seq errors)
      (do
        (run! println errors)
        (println (str "Usage:\n" summary)))

      enable
      (enable-systemd repo path user offset)

      disable
      (disable-systemd repo)

      command
      (systemctl (if all "*" repo) command)

      :else
      (do
        (println "You must indicate which action to take with either --disable or --enable.")
        (println (str "Usage:\n" summary)))))
  (shutdown-agents))
