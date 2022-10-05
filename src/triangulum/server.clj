(ns triangulum.server
  (:require
   [cider.nrepl             :refer [cider-nrepl-handler]]
   [clojure.java.io         :as io]
   [nrepl.core              :as nrepl]
   [nrepl.server            :as nrepl-server]
   [ring.adapter.jetty      :refer [run-jetty]]
   [triangulum.cli          :refer [get-cli-options]]
   [triangulum.config       :refer [get-config]]
   [triangulum.database     :refer [call-sql]]
   [triangulum.logging      :refer [log-str set-log-path!]]
   [triangulum.notify       :refer [available? ready!]]
   [triangulum.handler :refer [create-handler-stack]]
   [triangulum.utils :refer [get-ns]])
  (:import
   org.eclipse.jetty.server.Server))






(defonce ^:private server             (atom nil))
(defonce ^:private nrepl-server       (atom nil))
(defonce ^:private scheduling-service (atom nil))

(def ^:private expires-in "1 day in msecs" (* 1000 60 60 24))
(def ^:private ks-scan-interval 60) ; seconds
(def ^:private routing-handler-sym (-> (get-config :server) :routing))



;; TODO add scheduled jobs to config...
(defn- scheduled-jobs []
  (log-str "Closing old projects")
  (call-sql "close_old_projects" 30)
  (log-str "Checking for alerts")
  #_(send-email-alerts))

(defn- start-scheduling-service! []
  (log-str "Starting scheduling service.")
  (future
    (while true
      (Thread/sleep expires-in)
      (try (scheduled-jobs)
           (catch Exception _)))))

(def ^:private cli-options
  {:http-port   ["-p" "--http-port PORT"  "Port for http, default 8080"
                 :parse-fn #(if (int? %) % (Integer/parseInt %))]
   :https-port  ["-P" "--https-port PORT" "Port for https (e.g. 8443)"
                 :parse-fn #(if (int? %) % (Integer/parseInt %))]
   :nrepl       ["-r" "--nrepl" "Starts an nREPL server on port 5555"
                 :default false]
   :cider-nrepl ["-c" "--cider-nrepl" "Starts a CIDER nREPL server on port 5555"
                 :default false]
   :mode        ["-m" "--mode MODE" "Production (prod) or development (dev) mode, default prod"
                 :default "prod"
                 :validate [#{"prod" "dev"} "Must be \"prod\" or \"dev\""]]
   :log-dir     ["-l" "--log-dir DIR" "Directory for log files. When a directory is not provided, output will be to stdout."
                 :default ""]})

(def ^:private cli-actions
  {:start  {:description "Starts the server."
            :requires    [:http-port]}
   :stop   {:description "Stops the server."}
   :reload {:description "Reloads a running server."}})


(defn start-server! [{:keys [http-port https-port mode log-dir nrepl cider-nrepl]}]
  ;; Load the routing ns

  (let [_        (require (get-ns routing-handler-sym))
        has-key? (.exists (io/file "./.key/keystore.pkcs12"))
        ssl?     (and has-key? https-port)
        handler  (create-handler-stack (resolve routing-handler-sym) ssl? (= mode "dev"))
        config   (merge
                   {:port  http-port
                    :join? false}
                   (when ssl?
                     {:ssl?             true
                      :ssl-port         https-port
                      :keystore         "./.key/keystore.pkcs12"
                      :keystore-type    "pkcs12"
                      :ks-scan-interval ks-scan-interval
                      :key-password     "foobar"}))]
    (cond
      (and (not has-key?) https-port)
      (do (println "ERROR:\n"
                   "  An SSL key is required if an HTTPS port is specified.\n"
                   "  Create an SSL key for HTTPS or run without the --https-port (-P) option.")
          (System/exit 1))

      (and nrepl cider-nrepl)
      (do
        (println "ERROR:\n"
                 "  You can not use both --nrepl and --cider-nrepl together.")
        (System/exit 1))

      :else
      (do (when nrepl
            (println "Starting nREPL server on port 5555")
            (reset! nrepl-server (nrepl-server/start-server :port 5555)))
          (when cider-nrepl
            (println "Starting CIDER nREPL server on port 5555")
            (reset! nrepl-server (nrepl-server/start-server :port 5555 :handler cider-nrepl-handler)))
          (reset! server (run-jetty handler config))
          (reset! scheduling-service (start-scheduling-service!))
          (set-log-path! log-dir)
          (when (available?) (ready!))))))

(defn stop-server! []
  (set-log-path! "")
  (when @scheduling-service
    (future-cancel @scheduling-service)
    (reset! scheduling-service nil))
  (when @server
    (.stop ^Server @server)
    (reset! server nil)
    (System/exit 0)))

(defn send-to-nrepl-server! [msg & {:keys [host port] :or {host "127.0.0.1" port 5555}}]
  (try
    (with-open [conn ^nrepl.server.Server (nrepl/connect :host host :port port)]
      (-> (nrepl/client conn 1000)   ; message receive timeout required
          (nrepl/message {:op "eval" :code msg})
          nrepl/response-values))
    (catch Exception _
      (println (format "Unable to connect to nREPL server at %s:%s. Restart the server with the '-r/--nrepl' flag." host port))
      (System/exit 1)))
  (System/exit 0))

(defn stop-running-server! []
  (send-to-nrepl-server! "(do (require '[triangulum.server :as server]) (server/stop-server!))"))

(defn reload-running-server! []
  (send-to-nrepl-server! "(require 'triangulum.server :reload-all)"))

(defn -main [& args]
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
