(ns triangulum.handler-test
  (:require [clojure.string     :as str]
            [clojure.test       :refer [deftest is testing]]
            [triangulum.config  :refer [get-config]]
            [triangulum.handler :refer [authenticated-routing-handler
                                        create-handler-stack
                                        wrap-request-logging
                                        wrap-response-logging]]
            [triangulum.logging :refer [log log-str]]))

(defn- stub-config
  "get-config stub: returns m's value keyed by the config keyword; unset keys return nil."
  [m]
  (fn [& ks] (get m (first ks))))

(defn- run-request
  "Build the stack under config `m`, send one GET that writes a session, return the ring response."
  [m]
  (with-redefs [get-config (stub-config m)]
    (let [handler (create-handler-stack (fn [_] {:status 200 :session {:a 1}}) false false)]
      (handler {:request-method :get :uri "/" :scheme :http
                :server-name "localhost" :server-port 80 :remote-addr "127.0.0.1" :headers {}}))))

(defn- cookie-attrs [resp]
  (-> (get-in resp [:headers "Set-Cookie"]) first (str/split #";") (->> (map str/trim) set)))

(deftest ^:unit session-cookie-attrs-test
  (testing "default: cookie is unchanged - HttpOnly (ring default), no Secure"
    (let [attrs (cookie-attrs (run-request {}))]
      (is (contains? attrs "HttpOnly"))
      (is (not (contains? attrs "Secure")))))
  (testing "configured :session-cookie-attrs flow onto the session cookie"
    (let [attrs (cookie-attrs (run-request {:triangulum.handler/session-cookie-attrs
                                            {:secure true :same-site :lax}}))]
      (is (contains? attrs "Secure"))
      (is (contains? attrs "SameSite=Lax"))
      (is (contains? attrs "HttpOnly")))))   ; ring keeps HttpOnly under the merge

(deftest ^:unit hsts-header-test
  (testing "default: no HSTS header"
    (is (nil? (get-in (run-request {}) [:headers "Strict-Transport-Security"]))))
  (testing ":hsts? true sends Strict-Transport-Security"
    (is (some? (get-in (run-request {:triangulum.handler/hsts? true})
                       [:headers "Strict-Transport-Security"])))))

(defn- stub-server-config
  "get-config stub for the (get-config :server k) form; unset keys return nil."
  [m]
  (fn [_ k] (get m k)))

(defn- logged-body
  "The line logged for `body` served as `content-type` under `private-keys`."
  [content-type body private-keys]
  (let [line (atom nil)]
    (with-redefs [get-config (stub-server-config {:private-response-keys private-keys})
                  log-str    (fn [& parts] (reset! line (apply str parts)))]
      ((wrap-response-logging (fn [_] {:status  200
                                       :headers {"Content-Type" content-type}
                                       :body    body}))
       {}))
    @line))

(defn- logged-request
  "The line logged for a request carrying `params` under `private-keys`."
  [params private-keys]
  (let [line (atom nil)]
    (with-redefs [get-config (stub-server-config {:private-request-keys private-keys})
                  log        (fn [s & _] (reset! line s))]
      ((wrap-request-logging (fn [_] {:status 200}))
       {:request-method :post :uri "/x" :params params}))
    @line))

(deftest ^:unit private-response-keys-test
  (testing "configured keys are dropped from a map body"
    (is (= "Response(200): {:b 2}" (logged-body "application/edn" (pr-str {:secret "s3cr3t" :b 2}) #{:secret}))))
  (testing "a non-map body is logged as it is, rather than throwing"
    (is (= "Response(200): ok" (logged-body "application/edn" (pr-str "ok") #{:secret})))))

(deftest ^:unit private-keys-either-spelling-test
  (testing "a key written as a string still matches keywordized request params"
    (is (= "Request(post): \"/x\" {:b 1}" (logged-request {:password "hunter2" :b 1} #{"password"}))))
  (testing "a key written as a keyword still matches a JSON body's string keys"
    (is (= "Response(200): {b 2}"
           (logged-body "application/json" "{\"secret\":\"s3cr3t\",\"b\":2}" #{:secret}))))
  (testing "a namespaced key matches its full name, not the bare one"
    (is (= "Response(200): {b 2}"
           (logged-body "application/json" "{\"a/b\":\"s3cr3t\",\"b\":2}" #{:a/b})))))

;;; Refusal

(def gated-routes
  "One route nobody will be allowed through."
  {[:get "/gated"] {:auth-type :member :handler (fn [_] {:status 200})}})

(defn turns-everyone-away
  "A route-authenticator that always says no, so every request reaches the
   refusal branch."
  [_ _]
  false)

(defn explaining-the-ended-session
  "What an application registers when it can say more than \"Forbidden\"."
  [_]
  {:status 401 :body "Your session has ended. Please log in again."})

(defn nothing-here
  "A not-found-handler, distinct from every other answer so a test can tell
   which branch produced the response."
  [_]
  {:status 404 :body "Not Found"})

(defn- refuse
  "Send one request at a gated route under config `m` and return the response."
  [m]
  (with-redefs [get-config (stub-config m)]
    (authenticated-routing-handler {:request-method :get :uri "/gated"})))

(def ^:private refusing-config
  {:triangulum.handler/routing-tables      [`gated-routes]
   :triangulum.handler/route-authenticator `turns-everyone-away
   :triangulum.handler/redirect-handler    `explaining-the-ended-session
   :triangulum.handler/not-found-handler   `nothing-here})

(deftest ^:unit refused-handler-test
  (testing "default: a refused request is answered by forbidden-response, as before"
    (let [resp (refuse refusing-config)]
      (is (= 403 (:status resp)))
      (is (str/includes? (str (:body resp)) "Forbidden"))))
  (testing "a registered :refused-handler answers the refusal instead"
    (let [resp (refuse (assoc refusing-config
                              :triangulum.handler/refused-handler
                              `explaining-the-ended-session))]
      (is (= 401 (:status resp)))
      (is (str/includes? (:body resp) "session has ended"))))
  (testing "the other branches are untouched: an unknown route is still not-found's"
    (with-redefs [get-config (stub-config (assoc refusing-config
                                                 :triangulum.handler/refused-handler
                                                 `explaining-the-ended-session))]
      (is (= 404 (:status (authenticated-routing-handler
                           {:request-method :get :uri "/no-such-route"})))))))
