(ns triangulum.errors
  (:require [triangulum.logging :refer [log]]))

(defn init-throw
  "Throws an error message"
  [message]
  (throw (ex-info message {:causes [message]})))

(defn try-catch-throw
  "Runs a function and, in case it throws an exception, catches and logs the exception, then rethrows it with an enhanced input message."
  [try-fn message]
  (try (try-fn)
       (catch Exception e
         (log (ex-message e) :truncate? false)
         (let [causes (conj (:causes (ex-data e) []) message)]
           (throw (ex-info message {:causes causes}))))))

(defmacro nil-on-error
  "Catches exception and returns nil if its body throws an exception."
  [& body]
  `(try ~@body (catch Exception e# nil)))
