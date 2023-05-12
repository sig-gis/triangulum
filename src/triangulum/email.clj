(ns triangulum.email
  (:require [clojure.spec.alpha :as s]
            [postal.core        :refer [send-message]]
            [triangulum.config  :as config :refer [get-config]]
            [triangulum.logging :refer [log-str]]))

;; spec

(s/def ::host ::config/hostname)
(s/def ::user ::config/string)
(s/def ::pass ::config/string)
(s/def ::port ::config/port)

(defn get-base-url
  "Gets the homepage url."
  []
  (:base-url (get-config :mail)))

(defn email?
  "Checks if string is email."
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
  "Sends an email with a given subject and body to specified recipients.

  This function uses the `send-postal` internal function to send the email.
  It logs any errors that occur during sending.

  Arguments:
  to-addresses   - a collection of email addresses to which the email will be sent
  cc-addresses   - a collection of email addresses to which the email will be carbon copied
  bcc-addresses  - a collection of email addresses to which the email will be blind carbon copied
  subject        - a string representing the subject of the email
  body           - a string representing the body of the email
  content-type   - a keyword indicating the content type of the email, either :text for 'text/plain' or :html for 'text/html'

  Returns:
  Nothing. This function is designed for side-effects (i.e., it sends an email and potentially logs errors)."
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
