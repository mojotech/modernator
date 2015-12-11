(ns modernator.models.mailer
  (:require [postal.core :as postal]))

(defn send-message! [message]
  (let [smtp-host (System/getenv "SMTP_HOST")
        smtp-user (System/getenv "SMTP_USER")
        smtp-pass (System/getenv "SMTP_PASS")]
    (if smtp-host
      (postal/send-message
        {:host smtp-host
         :user smtp-user
         :pass smtp-pass}
        (merge
          {:from "jake+jeff@mojotech.com"
           :body [{:type "text/html"
                   :content (:content message)}]}
          (select-keys message [:from :to :subject])))
      (println message))))
