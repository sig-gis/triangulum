(ns triangulum.server
  (:import org.eclipse.jetty.server.Server)
  (:require [cider.nrepl         :refer [cider-nrepl-handler]]
            [clojure.java.io     :as io]
            [nrepl.core          :as nrepl]
            [nrepl.server        :as nrepl-server]
            [ring.adapter.jetty  :refer [run-jetty]]
            [triangulum.cli      :refer [get-cli-options]]
            [triangulum.config   :refer [get-config]]
            [triangulum.handler  :refer [create-handler-stack]]
            [triangulum.logging  :refer [log-str set-log-path!]]
            [triangulum.notify   :refer [available? ready!]]
            [triangulum.utils    :refer [resolve-foreign-symbol]]))

(defonce ^:private server       (atom nil))
(defonce ^:private nrepl-server (atom nil))
(defonce ^:private workers      (atom {}))

(def ^:private ks-scan-interval 60) ; seconds

;;===============================================
;; Workers
;;===============================================

(defn- start-workers! [worker-map]
  (reset! workers
          (reduce-kv (fn [acc worker-name {:keys [start]}]
                       (let [value (try
                                     (let [start-fn (resolve-foreign-symbol start)]
                                       (start-fn))
                                     (catch Exception e
                                       (log-str "Error starting worker "
                                                worker-name ": " (ex-message e))
                                       e))]
                         (assoc-in acc [worker-name :value] value)))
                     worker-map
                     worker-map)))

(defn- stop-workers! []
  (doseq [[worker-name {:keys [stop value]}] @workers]
    (when (and stop (not (instance? Exception value)))
      (try
        (let [stop-fn (resolve-foreign-symbol stop)]
          (stop-fn value))
        (catch Exception e
          (log-str "Error stopping worker " worker-name ": " (ex-message e)))))))

;;===============================================
;; Actions
;;===============================================

#_{:clj-kondo/ignore [:shadowed-var]}
(defn start-server!
  "FIXME: Write docstring"
  [{:keys [http-port https-port nrepl-port cider-nrepl-port mode log-dir handler
           workers keystore-file keystore-type keystore-password]}]
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

      (and nrepl-port cider-nrepl-port)
      (do
        (println "ERROR:\n"
                 "  You cannot use both --nrepl-port and --cider-nrepl-port together.")
        (System/exit 1))

      :else
      (do
        (when nrepl-port
          (println "Starting nREPL server on port" nrepl-port)
          (reset! nrepl-server (nrepl-server/start-server :port nrepl-port)))
        (when cider-nrepl-port
          (println "Starting CIDER nREPL server on port" cider-nrepl-port)
          (reset! nrepl-server (nrepl-server/start-server :port cider-nrepl-port :handler cider-nrepl-handler)))
        (when (seq workers)
          (println "Starting worker jobs")
          (start-workers! workers))
        (reset! server (run-jetty handler-stack config))
        (set-log-path! log-dir)
        (when (available?) (ready!))))))

(defn stop-server!
  "Stops server with workers jobs"
  []
  (set-log-path! "")
  (when @server
    (.stop ^Server @server)
    (reset! server nil))
  (stop-workers!)
  (System/exit 0))

(defn send-to-nrepl-server!
  "Sends form to the nrepl server"
  [msg & {:keys [host port] :or {host "127.0.0.1" port 5555}}]
  (try
    (with-open [conn ^nrepl.server.Server (nrepl/connect :host host :port port)]
      (-> (nrepl/client conn 1000)  ; message receive timeout required
          (nrepl/message {:op "eval" :code msg})
          nrepl/response-values))
    (catch Exception _
      (println (format "Unable to connect to nREPL server at %s:%s. Restart the server with the '-r/--nrepl-port' flag." host port))
      (System/exit 1)))
  (System/exit 0))

(defn stop-running-server!
  "Sends stop-server! call to the nrepl server"
  []
  (send-to-nrepl-server! "(do (require '[triangulum.server :as server]) (server/stop-server!))"))

(defn reload-running-server!
  "Reloads the server namespace"
  []
  (send-to-nrepl-server! "(require 'triangulum.server :reload-all)"))

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
   :nrepl-port       ["-r" "--nrepl-port PORT" "Port for an nREPL server (e.g., 5555)"
                      :parse-fn ensure-int]
   :cider-nrepl-port ["-c" "--cider-nrepl-port PORT" "Port for a CIDER nREPL server (e.g., 5555)"
                      :parse-fn ensure-int]
   :mode             ["-m" "--mode MODE" "Production (prod) or development (dev) mode, default prod"
                      :default "prod"
                      :validate [#{"prod" "dev"} "Must be \"prod\" or \"dev\""]]
   :log-dir          ["-l" "--log-dir DIR" "Directory for log files. When a directory is not provided, output will be to stdout."
                      :default ""]})

(def ^:private cli-actions
  {:start  {:description "Starts the server."
            :requires    [:http-port]}
   :stop   {:description "Stops the server."}
   :reload {:description "Reloads a running server."}})

(defn -main
  "Server entry main function"
  [& args]
  (if-let [{:keys [action options]} (get-cli-options args
                                                     cli-options
                                                     cli-actions
                                                     "server"
                                                     (get-config :server))]
    (case action
      :start  (start-server! options)
      :stop   (stop-running-server!)
      :reload (reload-running-server!)
      nil)
    (System/exit 1)))
