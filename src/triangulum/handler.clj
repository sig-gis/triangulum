(ns triangulum.handler
  (:require
   [clojure.edn                        :as edn]
   [clojure.data.json                  :as json]
   [clojure.set                        :as set]
   [clojure.string                     :as str]
   [ring.util.codec                    :refer [url-decode]]
   [ring.middleware.absolute-redirects :refer [wrap-absolute-redirects]]
   [ring.middleware.content-type       :refer [wrap-content-type]]
   [ring.middleware.default-charset    :refer [wrap-default-charset]]
   [ring.middleware.gzip               :refer [wrap-gzip]]
   [ring.middleware.json               :refer [wrap-json-params]]
   [ring.middleware.keyword-params     :refer [wrap-keyword-params]]
   [ring.middleware.multipart-params   :refer [wrap-multipart-params]]
   [ring.middleware.nested-params      :refer [wrap-nested-params]]
   [ring.middleware.not-modified       :refer [wrap-not-modified]]
   [ring.middleware.params             :refer [wrap-params]]
   [ring.middleware.reload             :refer [wrap-reload]]
   [ring.middleware.resource           :refer [wrap-resource]]
   [ring.middleware.session            :refer [wrap-session]]
   [ring.middleware.session.cookie     :refer [cookie-store]]
   [ring.middleware.ssl                :refer [wrap-ssl-redirect]]
   [ring.middleware.x-headers          :refer [wrap-content-type-options
                                               wrap-frame-options
                                               wrap-xss-protection]]
   [triangulum.config                  :refer [get-config]]
   [triangulum.logging                 :refer [log-str]]
   [triangulum.errors                  :refer [nil-on-error]]
   [triangulum.utils                   :refer [resolve-foreign-symbol]]
   [triangulum.response                :refer [forbidden-response data-response]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom Middlewares
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-bad-uri
  "Wrapper that checks if the request url contains a bad token from give as input set and returns a forbidden-response if there is or runs the next handler on it"
  [handler bad-tokens]
  (fn [request]
    (if (some #(str/includes? (str/lower-case (:uri request)) %)
              bad-tokens)
      (forbidden-response nil)
      (handler request))))

(defn wrap-request-logging
  "Wrapper that logs the incoming requests"
  [handler]
  (fn [request]
    (let [{:keys [uri request-method params]} request
          param-str                           (pr-str (dissoc params :password :passwordConfirmation))]
      (log-str "Request(" (name request-method) "): \"" uri "\" " param-str)
      (handler request))))

(defn wrap-response-logging
  "Wrapper that logs served responses"
  [handler]
  (fn [request]
    (let [{:keys [status headers body] :as response} (handler request)
          content-type                               (headers "Content-Type")]
      (log-str "Response(" status "): "
               (cond
                 (instance? java.io.File body)
                 (str content-type " file")

                 (= content-type "application/edn")
                 (binding [*print-length* 2] (print-str (edn/read-string body)))

                 (= content-type "application/json")
                 (binding [*print-length* 2] (print-str (nil-on-error (json/read-str body))))

                 :else
                 (str content-type " response")))
      response)))


(def updatable-session-keys
  "A vector containing session keys that can be updated"
  [])

(defn wrap-persistent-session
  "Wrapper to manage session"
  [handler]
  (fn [request]
    (let [{:keys [params session]} request
          to-update                (select-keys params updatable-session-keys)
          session                  (apply dissoc session (keys to-update))
          intersection             (set/intersection (set (keys params)) (set (keys session)))
          response                 (handler (update request :params merge session))]
      (when-not (empty? intersection)
        (log-str "WARNING! The following params are being overwritten by session values: " intersection))
      (if (and (contains? response :session)
               (nil? (:session response)))
        response
        (update response :session #(merge session to-update %))))))

(defn wrap-exceptions
  "Wrapper to manage exception handling, logging it and responding with 500 in case of an exception"
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [{:keys [data cause]} (Throwable->map e)
              status               (:status data)]
          (log-str "Error: " cause)
          (data-response cause {:status (or status 500)}))))))

(defn parse-query-string
  "Parses query strings and returns a params map"
  [query-string]
  (let [keyvals (-> (url-decode query-string)
                    (str/split #"&"))]
    (reduce (fn [params keyval]
              (->> (str/split keyval #"=")
                   (map edn/read-string)
                   (apply assoc params)))
            {}
            keyvals)))

(defn wrap-edn-params
  "Wrapper that parses request query strings and puts in :params request map"
  [handler]
  (fn [{:keys [content-type request-method query-string body params] :as request}]
    (if (= content-type "application/edn")
      (let [get-params  (when (and (= request-method :get)
                                   (not (str/blank? query-string)))
                          (parse-query-string query-string))
            post-params (when (and (= request-method :post)
                                   (not (nil? body)))
                          (edn/read-string (slurp body)))]
        (handler (assoc request :params (merge params get-params post-params))))
      (handler request))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler Stack
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn optional-middleware
  "Conditionally apply a middleware"
  [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn- string-to-bytes [^String s] (.getBytes s))

(defn create-handler-stack
  "Create the Ring handler stack"
  [routing-handler ssl? reload?]
  (-> routing-handler
      (optional-middleware wrap-ssl-redirect ssl?)
      (wrap-bad-uri (get-config :server :bad-tokens))
      wrap-request-logging
      wrap-persistent-session
      wrap-keyword-params
      wrap-json-params
      wrap-edn-params
      wrap-nested-params
      wrap-multipart-params
      wrap-params
      (wrap-session {:store
                     (cookie-store {:key
                                    (string-to-bytes (get-config :server :session-key))})})
      wrap-absolute-redirects
      (wrap-resource "public")
      wrap-content-type
      (wrap-default-charset "utf-8")
      wrap-not-modified
      (wrap-xss-protection true {:mode :block})
      (wrap-frame-options :sameorigin)
      (wrap-content-type-options :nosniff)
      wrap-response-logging
      wrap-gzip
      wrap-exceptions
      (optional-middleware wrap-reload reload?)))

(def development-app
  "Creates a development handler stack using the given config.edn handler with an active wrap-reload middleware"
  (create-handler-stack
   (-> (get-config :server :handler) resolve-foreign-symbol)
   false
   true))
