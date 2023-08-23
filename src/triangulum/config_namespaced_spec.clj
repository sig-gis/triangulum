(ns triangulum.config-namespaced-spec
  (:require [clojure.spec.alpha :as s]))

;; New Format (namespaced)

(s/def ::server   (s/keys :req [:triangulum.server/http-port
                                :triangulum.server/mode
                                :triangulum.server/log-dir
                                :triangulum.server/handler
                                :triangulum.handler/session-key
                                :triangulum.response/response-type]
                          :opt [:triangulum.server/https-port
                                :triangulum.server/nrepl
                                :triangulum.server/nrepl-port
                                :triangulum.server/nrepl-bind
                                :triangulum.server/cider-nrepl
                                :triangulum.server/keystore-file
                                :triangulum.server/keystore-type
                                :triangulum.server/keystore-password
                                :triangulum.handler/private-request-keys
                                :triangulum.handler/private-response-keys
                                :triangulum.handler/bad-tokens
                                :triangulum.worker/workers]))

(s/def ::app      (s/keys :opt [:triangulum.views/title
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

(s/def ::database (s/keys :req [:triangulum.database/dbname
                                :triangulum.database/user
                                :triangulum.database/password]
                          :opt [:triangulum.database/host
                                :triangulum.database/port
                                :triangulum.build-db/admin-pass
                                :triangulum.build-db/dev-data
                                :triangulum.build-db/file
                                :triangulum.build-db/verbose]))

(s/def ::mail     (s/keys :req [:triangulum.email/host
                                :triangulum.email/user
                                :triangulum.email/pass]
                          :opt [:triangulum.email/port]))

(s/def ::https    (s/keys :req [:triangulum.https/email
                                :triangulum.https/domain]
                          :opt [:triangulum.https/path
                                :triangulum.https/cert-only
                                :triangulum.https/webroot]))
