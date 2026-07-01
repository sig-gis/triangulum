(ns triangulum.handler-test
  (:require [clojure.string     :as str]
            [clojure.test       :refer [deftest is testing]]
            [triangulum.config  :refer [get-config]]
            [triangulum.handler :refer [create-handler-stack]]))

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
