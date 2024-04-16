(ns triangulum.sockets
  "Provides functionality for creating and managing client and server sockets. It includes functions for opening and checking socket connections, sending messages to the server, and starting/stopping socket servers with custom request handlers. This namespace enables communication between distributed systems and allows you to implement networked applications."
  (:import (java.io  BufferedReader)
           (java.net Socket ServerSocket))
  (:require [clojure.java.io   :as io]
            [clojure.string    :as s]
            [triangulum.logging :refer [log log-str]]))

;;=================================
;; Client Socket
;;=================================

(defn socket-open?
  "Checks if the socket at host/port is open."
  [host port]
  (try
    (with-open [_ (Socket. host port)]
      true)
    (catch java.net.ConnectException _
      false)))

(defn send-to-server!
  "Attempts to send socket message. Returns :success if successful."
  [host port message]
  (try
    (with-open [socket (Socket. host port)]
      (doto (io/writer socket)
        (.write (-> message
                    (s/trim-newline)
                    (str "\n")))
        (.flush))
      (.shutdownOutput socket))
    :success
    (catch java.net.ConnectException _
      (log-str "Connection to " host ":" port "failed"))))

;;=================================
;; Server Socket
;;=================================

(defonce ^:private global-server-thread (atom nil))
(defonce ^:private global-server-socket (atom nil))

(defn- read-socket! [^Socket socket]
  (.readLine ^BufferedReader (io/reader socket)))

(defn- accept-connections! [^ServerSocket server-socket handler]
  (while @global-server-thread
    (try
      (let [socket (.accept server-socket)]
        (try
          (->> (read-socket! socket)
               (handler))
          (catch Exception e
            (log-str "Server error: " e))
          (finally (.close socket))))
      (catch Exception _))))

(defn stop-socket-server!
  "Stops the socket server at port with handler."
  []
  (if @global-server-thread
    (do
      (reset! global-server-thread nil)
      (when @global-server-socket
        (.close ^ServerSocket @global-server-socket)
        (reset! global-server-socket nil))
      (log "Server stopped."))
    (log "Server is not running.")))

(defn start-socket-server!
  "Starts a socket server at port with handler."
  [port handler]
  (log "Starting socket server.")
  (if @global-server-thread
    (log "Server is already running.")
    (reset! global-server-thread
            (future
              (try
                (with-open [server-socket (ServerSocket. port)]
                  (reset! global-server-socket server-socket)
                  (accept-connections! server-socket handler))
                (catch Exception e
                  (log-str "Error creating server socket: " e)
                  (stop-socket-server!)))))))
