(ns triangulum.errors
  (:require [triangulum.logging :refer [log]]))

(defn init-throw
  "Throws an error message"
  [message]
  (throw (ex-info message {:causes [message]})))

(defn try-catch-throw
  "Runs an function if it throws, catches the exception and logs it and then throws an exception with augmented input message"
  [try-fn message]
  (try (try-fn)
       (catch Exception e
         (log (ex-message e))
         (let [causes (conj (:causes (ex-data e) []) message)]
           (throw (ex-info message {:causes causes}))))))

(defmacro nil-on-error
  "Catches exception and returns nil if its body throws an exception"
  [& body]
  `(try ~@body (catch Exception e# nil)))
