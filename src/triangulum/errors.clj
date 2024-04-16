(ns triangulum.errors
  "Error handling utilities for the Triangulum application.
   It includes functions and macros to handle exceptions and log errors."
  (:require [triangulum.logging :refer [log]]))

(defn init-throw
  "Takes a `message` string as input and throws an exception with the provided message.

   Example: `(init-throw \"Error: Invalid input\"))`"
  ([message]
   (throw (ex-info message {})))
  ([message data]
   (throw (ex-info message data)))
  ([message data cause]
   (throw (ex-info message data cause))))

(defn try-catch-throw
  "Takes a function `try-fn` and a `message` string as input. Executes `try-fn` and,
   if it throws an exception, catches the exception, logs the error, and then throws
   an exception with the augmented input message.

  Example:
  ```clojure
  (try-catch-throw (fn [] (throw (ex-info \"Initial error\" {}))) \"Augmented error message\")
  ```"
  [try-fn message]
  (try (try-fn)
       (catch Exception e
         (log (ex-message e) :truncate? false)
         (let [causes (conj (:causes (ex-data e) []) message)]
           (throw (ex-info message {:causes causes}))))))

(defmacro nil-on-error
  "Catches exception and returns `nil` if its body throws an exception.

  Example:
  ```clojure
  (nil-on-error (/ 1 0)) ; Returns nil
  (nil-on-error (+ 2 3)) ; Returns 5
  ```"
  [& body]
  `(try ~@body (catch Exception e# nil)))
