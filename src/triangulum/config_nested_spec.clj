(ns triangulum.config-nested-spec
  (:require [clojure.spec.alpha :as s]))

;; Old Format (un-namespaced)
;; TODO add missing keys (reference the config.example.edn)

(s/def ::server   (s/keys :opt-un [;; server 
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

(s/def ::app      (s/keys :opt-un [;; views 
                                   :triangulum.views/title
                                   :triangulum.views/description
                                   :triangulum.views/keywords
                                   :triangulum.views/extra-head-tags
                                   :triangulum.views/gtag-id
                                   :triangulum.views/static-css-files
                                   :triangulum.views/static-js-files
                                   :triangulum.views/get-user-lang
                                   :triangulum.views/js-init
                                   :triangulum.views/cljs-init
                                   :triangulum.views/client-keys
                                   ;; git 
                                   :triangulum.views/tags-url]))

(s/def ::database (s/keys :req-un [:triangulum.database/dbname
                                   :triangulum.database/user
                                   :triangulum.database/password]
                          :opt-un [:triangulum.database/host
                                   :triangulum.database/port]))

(s/def ::mail     (s/keys :req-un [:triangulum.email/host
                                   :triangulum.email/user
                                   :triangulum.email/pass]
                          :opt-un [:triangulum.email/port]))

(s/def ::https    (s/keys :req-un [:triangulum.https/domain
                                   :triangulum.https/email]))