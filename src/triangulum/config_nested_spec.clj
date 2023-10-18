(ns triangulum.config-nested-spec
  (:require [clojure.spec.alpha :as s]))

;; Old Format (un-namespaced)

(s/def ::server   (s/keys :req-un [:triangulum.server/http-port
                                   :triangulum.server/mode
                                   :triangulum.server/log-dir
                                   :triangulum.server/handler
                                   :triangulum.handler/session-key
                                   :triangulum.response/response-type]
                          :opt-un [:triangulum.server/https-port
                                   :triangulum.server/nrepl
                                   :triangulum.server/nrepl-port
                                   :triangulum.server/nrepl-bind
                                   :triangulum.server/cider-nrepl
                                   :triangulum.server/keystore-file
                                   :triangulum.server/keystore-type
                                   :triangulum.server/keystore-password
                                   :triangulum.handler/not-found-handler
                                   :triangulum.handler/redirect-handler
                                   :triangulum.handler/route-authenticator
                                   :triangulum.handler/routing-tables
                                   :triangulum.handler/bad-tokens
                                   :triangulum.handler/private-request-keys
                                   :triangulum.handler/private-response-keys
                                   :triangulum.worker/workers]))

(s/def ::app      (s/keys :opt-un [:triangulum.views/title
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
                                   :triangulum.git/tags-url]))

(s/def ::database (s/keys :req-un [:triangulum.database/dbname
                                   :triangulum.database/user
                                   :triangulum.database/password]
                          :opt-un [:triangulum.database/host
                                   :triangulum.database/port
                                   :triangulum.build-db/admin-pass
                                   :triangulum.build-db/dev-data
                                   :triangulum.build-db/file
                                   :triangulum.build-db/verbose]))

(s/def ::mail     (s/keys :req-un [:triangulum.email/host
                                   :triangulum.email/user
                                   :triangulum.email/pass]
                          :opt-un [:triangulum.email/port]))

(s/def ::https    (s/keys :req-un [:triangulum.https/email
                                   :triangulum.https/domain]
                          :opt-un [:triangulum.https/path
                                   :triangulum.https/cert-only
                                   :triangulum.https/webroot]))
