(ns modernator.models.mailer
  (:require [postal.core :as postal]
            [modernator.config :refer [config]]))

(defn send-message! [message]
  (let [smtp-host (config :smtp-host)
        smtp-user (config :smtp-user)
        smtp-pass (config :smtp-pass)]
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
