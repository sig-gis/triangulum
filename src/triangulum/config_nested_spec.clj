(ns triangulum.config-nested-spec
  (:require [clojure.spec.alpha :as s]))

;; Old Format (un-namespaced)

(def server-req [:triangulum.server/http-port
                 :triangulum.server/mode
                 :triangulum.server/log-dir
                 :triangulum.server/handler
                 :triangulum.handler/session-key
                 :triangulum.response/response-type])

(def server-opt [:triangulum.server/https-port
                 :triangulum.server/nrepl
                 :triangulum.server/nrepl-port
                 :triangulum.server/nrepl-host ; TODO: use it in server
                 :triangulum.server/cider-nrepl
                 :triangulum.server/workers
                 :triangulum.server/keystore-file
                 :triangulum.server/keystore-type
                 :triangulum.server/keystore-password
                 :triangulum.handler/bad-tokens])
(s/def ::server   (s/keys :req-un server-req
                          :opt-un server-opt))

(def app-req [])
(def app-opt [:triangulum.views/title
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
              :triangulum.git/tags-url])
(s/def ::app      (s/keys  :req-un []
                           :opt-un app-opt))

(def database-req [:triangulum.database/dbname
                   :triangulum.database/user
                   :triangulum.database/password])
(def database-opt [:triangulum.database/host
                   :triangulum.database/port
                   :triangulum.build-db/admin-pass
                   :triangulum.build-db/dev-data
                   :triangulum.build-db/file
                   :triangulum.build-db/verbose])
(s/def ::database (s/keys :req-un database-req
                          :opt-un database-opt))
(def mail-req [:triangulum.email/host
               :triangulum.email/user
               :triangulum.email/pass])
(def mail-opt [:triangulum.email/port])
(s/def ::mail     (s/keys :req-un mail-req
                          :opt-un mail-opt))

(def https-req [:triangulum.https/email
                :triangulum.https/domain])
(def https-opt [:triangulum.https/path
                :triangulum.https/cert-only
                :triangulum.https/webroot])
(s/def ::https    (s/keys :req-un https-req
                          :opt-un https-opt))