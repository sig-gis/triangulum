(ns triangulum.email
  "Provides some functionality for sending email from an SMTP
   server. Given the configuration inside `:mail`:
   - `:host`     - Hostname of the SMTP server.
   - `:user`     - Email account to use via SMTP (and which emails will be addressed from)
   - `:pass`     - Password to use via SMTP.
   - `:port`     - Port to use for SMTP.
   - `:base-url` - The host's base url, used when sending links in emails."
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

(def ^:private mime-type
  {:text "text/plain"
   :html "text/html"})

(defn send-mail
  "Sends an email with a given subject and body to specified recipients.

  This function uses the `send-postal` internal function to send the email.
  It logs the success or failure of sending this email.

  Arguments:
  to-addresses   - a collection of email addresses to which the email will be sent
  cc-addresses   - a collection of email addresses to which the email will be carbon copied
  bcc-addresses  - a collection of email addresses to which the email will be blind carbon copied
  subject        - a string representing the subject of the email
  body           - a string representing the body of the email
  content-type   - a keyword indicating the content type of the email, either :text for 'text/plain' or :html for 'text/html'

  Returns:
  Result map returned by `send-postal`."
  [to-addresses cc-addresses bcc-addresses subject body content-type]
  (log-str (format "Sending email to %s: %s" to-addresses subject))
  (let [result (send-postal to-addresses
                            cc-addresses
                            bcc-addresses
                            subject
                            body
                            (get mime-type content-type))]
    (if (= :SUCCESS (result :error))
      (log-str "Email sending succeeded.")
      (log-str "Email sending failed with error: " (result :message)))
    result))
