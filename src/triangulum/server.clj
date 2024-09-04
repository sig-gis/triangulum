(ns triangulum.server
  (:gen-class)
  (:import org.eclipse.jetty.server.Server)
  (:require [cider.nrepl         :refer [cider-nrepl-handler]]
            [clojure.java.io     :as io]
            [clojure.spec.alpha  :as s]
            [nrepl.core          :as nrepl]
            [nrepl.server        :as nrepl-server]
            [ring.adapter.jetty  :refer [run-jetty]]
            [triangulum.cli      :refer [get-cli-options]]
            [triangulum.config   :as config :refer [get-config]]
            [triangulum.handler  :refer [create-handler-stack]]
            [triangulum.logging  :refer [set-log-path!]]
            [triangulum.notify   :refer [available? ready!]]
            [triangulum.utils    :refer [resolve-foreign-symbol]]
            [triangulum.worker   :refer [start-workers! stop-workers!]]))

;; spec

(s/def ::http-port         ::config/port)
(s/def ::https-port        ::config/port)
(s/def ::nrepl-port        ::config/port)
(s/def ::nrepl-bind        ::config/string)
(s/def ::nrepl             boolean?)
(s/def ::cider-nrepl       boolean?)
(s/def ::mode              (s/and ::config/string #{"dev" "prod"}))
(s/def ::log-dir           ::config/string)
(s/def ::truncate-request? ::config/boolean)
(s/def ::handler           ::config/namespaced-symbol)
(s/def ::keystore-file     ::config/string)
(s/def ::keystore-type     ::config/string)
(s/def ::keystore-password ::config/string)

;; state

(defonce ^:private server       (atom nil))
(defonce ^:private nrepl-server (atom nil))
(def ^:private ks-scan-interval 60) ; seconds

;;===============================================
;; Actions
;;===============================================

#_{:clj-kondo/ignore [:shadowed-var]}
(defn start-server!
  "See README.org -> Web Framework -> triangulum.server for details."
  [{:keys [http-port https-port nrepl cider-nrepl nrepl-bind nrepl-port mode log-dir
           truncate-request? handler workers keystore-file keystore-type keystore-password]
    :or   {nrepl-bind        "127.0.0.1"
           nrepl-port        5555
           keystore-file     "./.key/keystore.pkcs12"
           keystore-type     "pkcs12"
           keystore-password "foobar"
           log-dir           ""
           truncate-request? true
           mode              "prod"}}]
  (let [has-key?      (and keystore-file (.exists (io/file keystore-file)))
        ssl?          (and has-key? https-port)
        reload?       (= mode "dev")
        handler-stack (-> (resolve-foreign-symbol handler)
                          (create-handler-stack ssl? reload?))
        config        (merge
                       {:port  http-port
                        :join? false}
                       (when ssl?
                         {:ssl?             true
                          :ssl-port         https-port
                          :keystore         keystore-file
                          :keystore-type    keystore-type
                          :key-password     keystore-password
                          :ks-scan-interval ks-scan-interval}))]
    (cond
      (and https-port (not has-key?))
      (do (println "ERROR:\n"
                   "  An SSL key is required if an HTTPS port is specified.\n"
                   "  Create an SSL key for HTTPS or run without the --https-port (-P) option.")
          (System/exit 1))

      (and nrepl cider-nrepl)
      (do
        (println "ERROR:\n"
                 "  You cannot use both --nrepl and --cider-nrepl together.")
        (System/exit 1))

      :else
      (do
        (when nrepl
          (println "Starting nREPL server on" (str nrepl-bind ":" nrepl-port))
          (reset! nrepl-server (nrepl-server/start-server :bind nrepl-bind :port nrepl-port)))
        (when cider-nrepl
          (println "Starting CIDER nREPL server on" (str nrepl-bind ":" nrepl-port))
          (reset! nrepl-server (nrepl-server/start-server :bind nrepl-bind :port nrepl-port :handler cider-nrepl-handler)))
        (when (seq workers)
          (println "Starting worker jobs")
          (start-workers! workers))
        (reset! server (run-jetty handler-stack config))
        (set-log-path! log-dir)
        (when (available?) (ready!))))))

(defn stop-server!
  "Stops server with workers jobs."
  []
  (set-log-path! "")
  (when @server
    (.stop ^Server @server)
    (reset! server nil))
  (stop-workers!)
  (System/exit 0))

(defn send-to-nrepl-server!
  "Sends form to the nrepl server"
  [msg & [{:keys [host port] :or {host "127.0.0.1" port 5555}}]]
  (try
    (with-open [conn ^nrepl.server.Server (nrepl/connect :host host :port port)]
      (-> (nrepl/client conn 1000)  ; message receive timeout required
          (nrepl/message {:op "eval" :code msg})
          nrepl/response-values))
    (catch Exception _
      (println (format "Unable to connect to nREPL server at %s:%s. Restart the server with either the '-r/--nrepl' or '-c/--cider-nrepl' flag." host port))
      (System/exit 1)))
  (System/exit 0))

(defn stop-running-server!
  "Sends stop-server! call to the nrepl server."
  [{:keys [nrepl-bind nrepl-port]}]
  (send-to-nrepl-server! "(do (require '[triangulum.server :as server]) (server/stop-server!))"
                         (cond-> {}
                           nrepl-bind (assoc :host nrepl-bind)
                           nrepl-port (assoc :port nrepl-port))))

(defn reload-running-server!
  "Reloads the server namespace and its dependencies."
  [{:keys [nrepl-bind nrepl-port]}]
  (send-to-nrepl-server! "(require 'triangulum.server :reload-all)"
                         (cond-> {}
                           nrepl-bind (assoc :host nrepl-bind)
                           nrepl-port (assoc :port nrepl-port))))

;;===============================================
;; Argument parsing
;;===============================================

(defn- ensure-int [x]
  (if (int? x) x (Integer/parseInt x)))

(def ^:private cli-options
  {:http-port        ["-p" "--http-port PORT"  "Port for http (e.g., 8080)"
                      :parse-fn ensure-int]
   :https-port       ["-P" "--https-port PORT" "Port for https (e.g., 8443)"
                      :parse-fn ensure-int]
   :nrepl-port       ["-n" "--nrepl-port PORT" "Port for an nREPL server (e.g., 5555)"
                      :parse-fn ensure-int]
   :nrepl-bind       ["-b" "--nrepl-bind IP" "IP address to bind nREPL server (eg., 127.0.0.1)"]
   :nrepl            ["-r" "--nrepl" "Launch an nREPL server (on nrepl-port or 5555)"]
   :cider-nrepl      ["-c" "--cider-nrepl" "Launch a CIDER nREPL server (on nrepl-port or 5555)"]
   :mode             ["-m" "--mode MODE" "Production (prod) or development (dev) mode, default prod"
                      :default "prod"
                      :validate [#{"prod" "dev"} "Must be \"prod\" or \"dev\""]]
   :log-dir          ["-l" "--log-dir DIR" "Directory for log files. When a directory is not provided, output will be to stdout."
                      :default ""]})

(def ^:private cli-actions
  {:start  {:description "Starts the web server, nREPL server, logger, and workers."
            :requires    [:http-port]}
   :stop   {:description "Stops the web server, nREPL server, logger, and workers."}
   :reload {:description "Reloads namespaces into a running server."}})

(defn -main
  "Server entry main function."
  [& args]
  (if-let [{:keys [action options]} (get-cli-options args
                                                     cli-options
                                                     cli-actions
                                                     "server"
                                                     (get-config :server))]
    (case action
      :start  (start-server! options)
      :stop   (stop-running-server! options)
      :reload (reload-running-server! options)
      nil)
    (System/exit 0)))
