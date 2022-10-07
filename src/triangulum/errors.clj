(ns triangulum.errors
  (:require [triangulum.logging :refer [log]]))

(defn init-throw [message]
  (throw (ex-info message {:causes [message]})))

(defn try-catch-throw [try-fn message]
  (try (try-fn)
       (catch Exception e
         (log (ex-message e))
         (let [causes (conj (:causes (ex-data e) []) message)]
           (throw (ex-info message {:causes causes}))))))


(defmacro nil-on-error
  [& body]
  `(try ~@body (catch Exception e# nil)))
