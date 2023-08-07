(ns triangulum.worker
  (:require  [clojure.spec.alpha  :as s]
             [triangulum.logging  :refer [log-str]]
             [triangulum.utils    :refer [resolve-foreign-symbol]]))

;;spec

(s/def ::nested-worker     (s/keys :req-un [::start ::stop]))
(s/def ::namespaced-worker (s/keys :req [::name
                                         ::start
                                         ::stop]))
(s/def ::workers           (s/or :map    (s/map-of keyword? ::nested-worker)
                                 :vector (s/coll-of ::namespaced-worker :kind vector?)))
;; state

(defonce ^:private workers (atom {}))

;;===============================================
;; Actions
;;===============================================

#_{:clj-kondo/ignore [:shadowed-var]}
(defn- start-worker [name start]
  (try
    (let [start-fn (resolve-foreign-symbol start)]
      (start-fn))
    (catch Exception e
      (log-str "Error starting worker "
               name ": " (ex-message e))
      e)))

#_{:clj-kondo/ignore [:shadowed-var]}
(defmulti start-workers!
  "Starts a set of workers based on the provided configuration.
  The workers parameter can be either a map (for nested workers) or a vector (for namespaced workers).
  For nested workers, the map keys are worker names and values are maps with :start (a symbol representing the start function) and :stop keys. The start function is called to start the worker.
  For namespaced workers, the vector elements are maps with ::name (the worker name), ::start (a symbol representing the start function), and ::stop keys. The start function is called to start each worker.
  Arguments:
   workers - a map or vector representing the workers to be started."
  (fn [workers] (if (map? workers) :nested :namespaced)))


(defmethod start-workers! :nested [worker-map]
  (reset! workers
          (reduce-kv (fn [acc worker-name {:keys [start]}]
                       (let [value (start-worker worker-name start)]
                         (assoc-in acc [worker-name :value] value)))
                     worker-map
                     worker-map)))

#_{:clj-kondo/ignore [:shadowed-var]}
(defmethod start-workers! :namespaced [worker-vec]
  (reset! workers
          (mapv (fn [{::keys [name start] :as worker}]
                  (let [value (start-worker name start)]
                    (assoc worker ::value value)))
                worker-vec)))

#_{:clj-kondo/ignore [:shadowed-var]}
(defn- stop-worker! [name stop value]
  (when (and stop (not (instance? Exception value)))
    (try
      (let [stop-fn (resolve-foreign-symbol stop)]
        (stop-fn value))
      (catch Exception e
        (log-str "Error stopping worker " name ": " (ex-message e))))))

(defmulti stop-workers!
  "Stops a set of currently running workers.
  The workers to stop are determined based on the current state of the `workers` atom. If the `workers` atom contains a map, it's assumed to be holding nested workers. If it contains a vector, it's assumed to be holding namespaced workers.
  The stop function is called with the value to stop each worker."
  (fn [] (if (map? @workers) :nested :namespaced)))

(defmethod stop-workers! :nested []
  (doseq [[worker-name {:keys [stop value]}] @workers]
    (stop-worker! worker-name stop value)))

#_{:clj-kondo/ignore [:shadowed-var]}
(defmethod stop-workers! :namespaced []
  (doseq [{::keys [name stop value]} @workers]
    (stop-worker! name stop value)))
