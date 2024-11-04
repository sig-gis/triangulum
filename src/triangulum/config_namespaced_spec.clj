(ns triangulum.config-namespaced-spec
  (:require [clojure.spec.alpha :as s]))

;; Helper functions

(defn- key-namespaces
  [m]
  (set (map namespace (keys m))))

(defn- no-keys-of-ns?
  [m ns-string]
  (not (contains? (key-namespaces m) ns-string)))

;; New Format (namespaced)

(s/def ::server (s/or
                 :no-server-keys
                 #(no-keys-of-ns? % "triangulum.server")
                 :server-keys
                 (s/keys :req [:triangulum.server/http-port
                               :triangulum.server/handler]
                         :opt [:triangulum.server/https-port
                               :triangulum.server/nrepl
                               :triangulum.server/nrepl-port
                               :triangulum.server/nrepl-bind
                               :triangulum.server/cider-nrepl
                               :triangulum.server/keystore-file
                               :triangulum.server/keystore-type
                               :triangulum.server/keystore-password
                               :triangulum.server/mode
                               :triangulum.server/log-dir
                               :triangulum.handler/not-found-handler
                               :triangulum.handler/redirect-handler
                               :triangulum.handler/route-authenticator
                               :triangulum.handler/routing-tables
                               :triangulum.handler/truncate-request
                               :triangulum.handler/private-request-keys
                               :triangulum.handler/private-response-keys
                               :triangulum.handler/bad-tokens
                               :triangulum.handler/upload-max-size-mb
                               :triangulum.handler/upload-max-file-count
                               :triangulum.handler/cors-headers
                               :triangulum.worker/workers
                               :triangulum.response/response-type])))

(s/def ::app (s/keys :opt [:triangulum.views/title
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

(s/def ::database (s/or
                   :no-database-keys
                   #(no-keys-of-ns? % "triangulum.database")
                   :database-keys
                   (s/keys :req [:triangulum.database/dbname
                                 :triangulum.database/user
                                 :triangulum.database/password]
                           :opt [:triangulum.database/host
                                 :triangulum.database/port
                                 :triangulum.build-db/admin-pass
                                 :triangulum.build-db/dev-data
                                 :triangulum.build-db/file
                                 :triangulum.build-db/verbose])))

(s/def ::mail (s/or
               :no-email-keys
               #(no-keys-of-ns? % "triangulum.email")
               :email-keys
               (s/keys :req [:triangulum.email/host
                             :triangulum.email/user
                             :triangulum.email/pass]
                       :opt [:triangulum.email/port])))

(s/def ::https (s/or
                :no-https-keys
                #(no-keys-of-ns? % "triangulum.https")
                :https-keys
                (s/keys :req [:triangulum.https/email
                              :triangulum.https/domain]
                        :opt [:triangulum.https/path
                              :triangulum.https/cert-only
                              :triangulum.https/webroot])))
