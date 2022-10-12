(ns triangulum.handler
  (:require
   [clojure.data.json :as json]
   [clojure.edn       :as edn]
   [clojure.set       :as set]
   [clojure.string    :as str]
   [cognitect.transit  :as transit]
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
   [triangulum.errors                  :refer [nil-on-error]])
  (:import
   java.io.ByteArrayOutputStream))

;; Response building

(defn- clj->transit
  "Converts a clj body to transit."
  [body]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer body)
    (.toString out)))
#_{:clj-kondo/ignore [:shadowed-var]}
(def response-type (atom nil))

(defn data-response
  "Create a response object.
   Body is required. Status, type, and session are optional.
   When a type keyword is passed, the body is converted to that type,
   otherwise the body and type are passed through."
  ([body]
   (data-response body {}))
  ([body {:keys [status type session]
          :or   {status 200
                 type   (or @response-type
                            (reset! response-type (-> (triangulum.config/get-config :server) :response-type)))}
          :as   params}]
   (merge (when (contains? params :session) {:session session})
          {:status  status
           :headers {"Content-Type" (condp = type
                                      :edn     "application/edn"
                                      :transit "application/transit+json"
                                      :json    "application/json"
                                      type)}
           :body    (condp = type
                      :edn     (pr-str         body)
                      :transit (clj->transit  body)
                      :json    (json/write-str body)
                      body)})))

(defn json-response
  ([body] (data-response {:type :json}))
  ([body params] (data-response (merge {:type :json} params))))

(defn edn-response
  ([body] (data-response {:type :edn}))
  ([body params] (data-response (merge {:type :edn} params))))

(defn transit-response
  ([body] (data-response {:type :transit}))
  ([body params] (data-response (merge {:type :transit} params))))

(defn no-cross-traffic? [{:strs [referer host]}]
  (and referer host (str/includes? referer host)))

(defn forbidden-response [_]
  (data-response "Forbidden" {:status 403}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom Middlewares
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-bad-uri [handler]
  (fn [request]
    (if (str/includes? (str/lower-case (:uri request)) "php")
      (forbidden-response nil)
      (handler request))))

(defn wrap-request-logging [handler]
  (fn [request]
    (let [{:keys [uri request-method params]} request
          param-str                           (pr-str (dissoc params :password :passwordConfirmation))]
      (log-str "Request(" (name request-method) "): \"" uri "\" " param-str)
      (handler request))))

(defn wrap-response-logging [handler]
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


(def updatable-session-keys [])

(defn wrap-persistent-session [handler]
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

(defn wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [{:keys [data cause]} (Throwable->map e)
              status               (:status data)]
          (log-str "Error: " cause)
          (data-response cause {:status (or status 500)}))))))

(defn parse-query-string [query-string]
  (let [keyvals (-> (url-decode query-string)
                    (str/split #"&"))]
    (reduce (fn [params keyval]
              (->> (str/split keyval #"=")
                   (map edn/read-string)
                   (apply assoc params)))
            {}
            keyvals)))

(defn wrap-edn-params [handler]
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

(defn optional-middleware [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn- string-to-bytes [^String s] (.getBytes s))

(defn create-handler-stack [routing-handler ssl? reload?]
  (-> routing-handler
      (optional-middleware wrap-ssl-redirect ssl?)
      wrap-bad-uri
      wrap-request-logging
      wrap-persistent-session
      wrap-keyword-params
      wrap-json-params
      wrap-edn-params
      wrap-nested-params
      wrap-multipart-params
      wrap-params
      (wrap-session {:store (cookie-store {:key (string-to-bytes (get-config :session-key))})})
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
