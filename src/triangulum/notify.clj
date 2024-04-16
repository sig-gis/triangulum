(ns triangulum.notify
  "Provides functions to interact with systemd for process management and notifications.

   Uses the SDNotify Java library to send notifications and check the availability of the
   current process.

   The functions in this namespace allow you to check if the process is managed by systemd,
   send \"ready,\" \"reloading,\" and \"stopping\" messages, and send custom status messages.

   These functions can be helpful when integrating your application with systemd for
   better process supervision and management."
  (:import [info.faljse.SDNotify SDNotify]))

;; Notify
(defn available?
  "Checks if this process is a process managed by systemd."
  []
  (SDNotify/isAvailable))

(defn ready!
  "Sends ready message to systemd. Systemd file must include `Type=notify` to be used."
  []
  (SDNotify/sendNotify))

(defn reloading!
  "Sends reloading message to systemd. Must call `send-notify!` once reloading
   has been completed."
  []
  (SDNotify/sendReloading))

(defn stopping!
  "Sends stopping message to systemd."
  []
  (SDNotify/sendStopping))

(defn send-status!
  "Sends status message to systemd. (e.g. `(send-status! \"READY=1\")`)."
  [s]
  (SDNotify/sendStatus s))
