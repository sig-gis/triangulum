(ns triangulum.handler
  (:require [clojure.data.json                  :as json]
            [clojure.edn                        :as edn]
            [clojure.spec.alpha                 :as s]
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
            [triangulum.config                  :as config :refer [get-config]]
            [triangulum.logging                 :refer [log-str]]
            [triangulum.errors                  :refer [nil-on-error]]
            [triangulum.utils                   :refer [resolve-foreign-symbol]]
            [triangulum.response                :refer [forbidden-response data-response]]))

;; spec

(s/def ::session-key (s/and ::config/string #(= 16 (count %))))
(s/def ::bad-tokens  (s/coll-of ::config/string :kind set? :min-count 0))
(s/def ::private-request-keys (s/coll-of keyword :kind set?))
(s/def ::private-response-keys (s/coll-of keyword :kind set?))

;; state

;; FIXME: Make this into a reloadable component
(defonce ^:private session-cookie-store (atom nil))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routing Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn authenticated-routing-handler
  "Routing Handler that delegates authentication & redirection
   to handlers specified in your config.edn"
  [{:keys [uri request-method #_params #_session #_headers] :as request}]
  (let [redirect-handler  (resolve-foreign-symbol (get-config :triangulum.handler/redirect-handler))
        not-found-handler (resolve-foreign-symbol (get-config :triangulum.handler/not-found-handler))
        is-authenticated? (resolve-foreign-symbol (get-config :triangulum.handler/route-authenticator))
        routes            (->> (get-config :triangulum.handler/routing-tables)
                               (map resolve-foreign-symbol)
                               (apply merge))
        {:keys [auth-type auth-action handler] :as route} (get routes [request-method uri])]
    (cond
      (nil? route)                                                (not-found-handler request)
      (or (nil? auth-type) (is-authenticated? request auth-type)) (handler request)
      (= :redirect auth-action)                                   (redirect-handler request)
      :else                                                       (forbidden-response request))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom Middlewares
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn case-insensitive-substring?
  "True if s includes substr regardless of case."
  [s substr]
  (str/includes? (str/lower-case s)
                 (str/lower-case substr)))

(defn wrap-bad-uri
  "Wrapper that checks if the request url contains a bad token from the
  provided set and returns a forbidden-response if so; otherwise,
  passes the request to the provided handler."
  [handler]
  (fn [{:keys [uri] :as request}]
    (let [bad-tokens (get-config :server :bad-tokens)]
      (if (some #(case-insensitive-substring? uri %) bad-tokens)
        (forbidden-response request)
        (handler request)))))

(defn wrap-request-logging
  "Wrapper that logs the incoming requests."
  [handler]
  (fn [request]
    (let [{:keys [uri request-method params]} request
          private-request-keys                (or (get-config :server :private-request-keys)
                                                  #{:password :passwordConfirmation})
          param-str                           (pr-str (apply dissoc params private-request-keys))]
      (log-str "Request(" (name request-method) "): \"" uri "\" " param-str)
      (handler request))))

(defn wrap-response-logging
  "Wrapper that logs served responses."
  [handler]
  (fn [request]
    (let [{:keys [status headers body] :as response} (handler request)
          content-type                               (headers "Content-Type")
          private-response-keys                      (get-config :server :private-response-keys)]
      (log-str "Response(" status "): "
               (cond
                 (instance? java.io.File body)
                 (str content-type " file")

                 (= content-type "application/edn")
                 (binding [*print-length* 2] (print-str (apply dissoc (edn/read-string body) private-response-keys)))

                 (= content-type "application/json")
                 (binding [*print-length* 2] (print-str (apply dissoc (nil-on-error (json/read-str body)) private-response-keys)))

                 :else
                 (str content-type " response")))
      response)))

(defn wrap-exceptions
  "Wrapper to manage exception handling, logging it and responding with 500 in case of an exception."
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
  "Parses query strings and returns a params map."
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
  "Wrapper that parses request query strings and puts in :params request map."
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

(defn string-to-bytes
  "Converts a string into a byte array."
  [^String s]
  (.getBytes s))

(defn get-cookie-store
  "Computes a new `ring.middleware.session.cookie/cookie-store` object
  on the first call and caches it for use in all future calls."
  []
  (or @session-cookie-store
      (reset! session-cookie-store
              (cookie-store {:key (-> (get-config :server :session-key)
                                      (string-to-bytes))}))))

(defn wrap-wrap-session
  "Wrapper around `ring.middleware.session/wrap-session` that defers
  looking up the cookie store until a request has been made."
  [handler]
  (fn [request]
    (let [wrapped-handler (wrap-session handler {:store (get-cookie-store)})]
      (wrapped-handler request))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler Stack
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn optional-middleware
  "Conditionally apply a middleware."
  [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn create-handler-stack
  "Create the Ring handler stack."
  [routing-handler ssl? reload?]
  (-> routing-handler
      (optional-middleware wrap-ssl-redirect ssl?)
      wrap-bad-uri
      wrap-request-logging
      wrap-keyword-params
      wrap-json-params
      wrap-edn-params
      wrap-nested-params
      wrap-multipart-params
      wrap-params
      wrap-wrap-session ; used to prevent running `get-config` at file load time
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
  "Handler function for development (figwheel)."
  (create-handler-stack
   (fn [request]
     (let [user-handler (-> (get-config :server :handler)
                            resolve-foreign-symbol)]
       (user-handler request)))
   false
   true))
