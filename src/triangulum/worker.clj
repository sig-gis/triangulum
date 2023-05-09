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

(defn- start-worker [name start]
  (try
    (let [start-fn (resolve-foreign-symbol start)]
      (start-fn))
    (catch Exception e
      (log-str "Error starting worker "
               name ": " (ex-message e))
      e)))

(defmulti start-workers! (fn [workers] (if (map? workers) :nested :namespaced)))

(defmethod start-workers! :nested [worker-map]
  (reset! workers
          (reduce-kv (fn [acc worker-name {:keys [start]}]
                       (let [value (start-worker worker-name start)]
                         (assoc-in acc [worker-name :value] value)))
                     worker-map
                     worker-map)))


(defmethod start-workers! :namespaced [worker-vec]
  (reset! workers
          (reduce (fn [acc {::keys [name start]}]
                    (let [value (start-worker name start)]
                      (assoc acc ::value value)))
                  worker-vec
                  worker-vec)))

(defn- stop-worker! [name stop value]
  (when (and stop (not (instance? Exception value)))
    (try
      (let [stop-fn (resolve-foreign-symbol stop)]
        (stop-fn value))
      (catch Exception e
        (log-str "Error stopping worker " name ": " (ex-message e))))))

(defmulti stop-workers! (fn [] (if (map? @workers) :nested :namespaced)))

(defmethod stop-workers! :nested []
  (doseq [[worker-name {:keys [stop value]}] @workers]
    (stop-worker! worker-name stop value)))

(defmethod stop-workers! :namespaced []
  (doseq [{::keys [name stop value]} @workers]
    (stop-worker! name stop value)))