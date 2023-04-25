(ns triangulum.config-namespaced-spec
  (:require [clojure.spec.alpha :as s]))

;; New Format (namespaced)
;; TODO add missing keys (reference the config.example.edn)

(s/def ::server   (s/keys :opt [;; server 
                                :triangulum.server/http-port
                                :triangulum.server/https-port
                                :triangulum.server/nrepl
                                :triangulum.server/nrepl-port
                                :triangulum.server/nrepl-host
                                :triangulum.server/cider-nrepl
                                :triangulum.server/mode
                                :triangulum.server/log-dir
                                :triangulum.server/handler
                                :triangumum.server/workers
                                :triangumum.server/keystore-file
                                :triangumum.server/keystore-type
                                :triangumum.server/keystore-password
                                ;; handler
                                :triangumum.server/session-key
                                :triangumum.server/bad-tokens
                                ;; response
                                :triangumum.server/response-type]))

(s/def ::app      (constantly true))

(s/def ::database (s/keys :req [:triangulum.database/dbname
                                :triangulum.database/user
                                :triangulum.database/password]
                          :opt [:triangulum.database/host
                                :triangulum.database/port]))

(s/def ::mail     (s/keys :req [:triangulum.email/host
                                :triangulum.email/user
                                :triangulum.email/pass]
                          :opt [:triangulum.email/port]))

(s/def ::https    (s/keys :req [:triangulum.https/domain
                                :triangulum.https/email]))

