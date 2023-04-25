(ns triangulum.email
  (:require [triangulum.config  :as config :refer [get-config]]
            [triangulum.logging :refer [log-str]]
            [clojure.spec.alpha :as s]
            [postal.core        :refer [send-message]]))

;; spec

(s/def ::host ::config/hostname)
(s/def ::user ::config/string)
(s/def ::pass ::config/string)
(s/def ::port ::config/port)

(defn get-base-url
  "Gets the homepage url"
  []
  (:base-url (get-config :mail)))

(defn email?
  "Checks if string is email"
  [string]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (and (string? string) (re-matches pattern string))))

(defn- send-postal [to-addresses cc-addresses bcc-addresses subject body content-type]
  (send-message
   (select-keys (get-config :mail) [:host :user :pass :tls :port])
   {:from    (get-config :mail :user)
    :to      to-addresses
    :cc      cc-addresses
    :bcc     bcc-addresses
    :subject subject
    :body    [{:type    (or content-type "text/plain")
               :content body}]}))

(defn send-mail
  "Sends email (text or html) to given addresses"
  [to-addresses cc-addresses bcc-addresses subject body content-type]
  (let [mime                    {:text "text/plain"
                                 :html "text/html"}
        {:keys [message error]} (send-postal to-addresses
                                             cc-addresses
                                             bcc-addresses
                                             subject
                                             body
                                             (content-type mime))]
    (when-not (= :SUCCESS error) (log-str message))))
