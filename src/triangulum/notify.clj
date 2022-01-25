(ns triangulum.notify
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
