(ns triangulum.config-namespaced-spec
  (:require [clojure.spec.alpha :as s]
            [triangulum.config-nested-spec :refer [server-req server-opt
                                                   app-req app-opt
                                                   database-req database-opt
                                                   mail-req mail-opt
                                                   https-req https-opt]]))

;; New Format (namespaced)

(s/def ::server   (s/keys :req server-req
                          :opt server-opt))

(s/def ::app      (s/keys  :req app-req
                           :opt app-opt))

(s/def ::database (s/keys :req database-req
                          :opt database-opt))

(s/def ::mail     (s/keys :req mail-req
                          :opt mail-opt))

(s/def ::https    (s/keys :req https-req
                          :opt https-opt))