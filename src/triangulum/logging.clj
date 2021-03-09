(ns triangulum.logging
  (:import java.text.SimpleDateFormat
           java.util.Date
           java.io.File)
  (:require [clojure.java.io :as io]
            [clojure.pprint  :as pp]))

(defonce synchronized-log-writer (agent nil))
(defonce output-path             (atom ""))
(defonce clean-up-service        (atom nil))

(defn- max-length [string length]
  (subs string 0 (min length (count string))))

(defn log
  "Synchronously create a log entry. Logs will got to standard out as default.
   A log file location can be specified with set-log-path!.

   Default options are {:newline? true :pprint? false :force-stdout? false}"
  [data & {:keys [newline? pprint? force-stdout?]
           :or {newline? true pprint? false force-stdout? false}}]
  (let [timestamp    (.format (SimpleDateFormat. "MM/dd HH:mm:ss") (Date.))
        log-filename (str (.format (SimpleDateFormat. "YYYY-MM-dd") (Date.)) ".log")
        max-data     (max-length data 500)
        line         (str timestamp
                          " "
                          (if pprint? (with-out-str (pp/pprint data)) max-data)
                          (when (and newline? (not pprint?)) "\n"))]
    (send-off synchronized-log-writer
              (if (or force-stdout? (= "" @output-path))
                (fn [_] (print line) (flush))
                (fn [_] (spit (io/file @output-path log-filename) line :append true)))))
  nil)

(defn log-str
  "A variadic version of log which concatenates all of the strings into one log line.
   Uses the default options for log."
  [& data]
  (log (apply str data)))

(defn- start-clean-up-service! []
  (log "Starting log file removal service." :force-stdout? true)
  (future
    (while true
      (Thread/sleep (* 1000 60 60 24)) ; 24 hours in milliseconds.
      (try (doseq [file (as-> (io/file @output-path) files
                          (.listFiles files)
                          (sort-by (fn [^File f] (.lastModified f)) files)
                          (take (- (count files) 10) files))]
             (io/delete-file file))
           (catch Exception _)))))

(defn set-log-path!
  "Sets a path to create file logs. When set to a directory, log files will be
   created with the date as part of the file name. When an empty string is set
   logging will be sent to standard out."
  [path]
  (cond
    (pos? (count path))
    (try
      (io/make-parents (io/file path "dummy.log"))
      (reset! output-path path)
      (log (str "Logging to: " path) :force-stdout? true)
      (when (nil? @clean-up-service)
        (reset! clean-up-service (start-clean-up-service!)))
      (catch Exception _
        (reset! output-path "")
        (log (str "Error setting log path to " path ". Check that you supplied a valid path.") :force-stdout? true)))

    (not (nil? @clean-up-service))
    (do
      (reset! output-path "")
      (log "Logging to: stdout" :force-stdout? true)
      (future-cancel @clean-up-service)
      (reset! clean-up-service nil))))
