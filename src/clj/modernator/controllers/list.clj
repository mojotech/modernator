(ns modernator.controllers.list
  (:require [clojure.walk :refer [keywordize-keys]]
            [ring.util.response :as response]
            [clojure.string :as string]
            [modernator.views.main :as views]
            [modernator.models.mailer :as mailer]
            [modernator.models.main :as models]))

(defn create [req]
  (let [params (select-keys (keywordize-keys (:form-params req))
                            [:email :list :purpose :list_emails])
        owner (first (models/users-create [{:email (:email params)}]))
        m-list (models/list-create
                {:name (:list params)
                 :purpose (:purpose params)
                 :admin_id (:id owner)})]

    (models/users-create
      (mapv #(hash-map :email % :list_id (:id m-list))
            (string/split (:list_emails params) #" ")))

    (mailer/send-message!
      {:to (:email params)
       :subject "Get Started with Modernator!"
       :content (views/admin-welcome-email (:auth_token owner))})

    (models/user-update {:list_id (:id m-list) :was_invited true} ["id=?" (:id owner)])

    (response/redirect "/signup-confirm")))

(defn activate [auth-token]
  (let [user (models/user-find "auth_token" auth-token)]
    (if (nil? user)
      (response/response "What the hell?")
      (do
        (models/user-update {:is_verified true} ["id = ?" (:id user)])

        (pmap #(when (not= (:was_invited (models/user-find "id" (:id %))) true)
                 (mailer/send-message!
                   {:to (:email %)
                    :subject "You Were Invited to Collaborate!"
                    :content (views/user-welcome-email (:auth_token %))})
                 (models/user-update {:was_invited true} ["id = ?" (:id %)]))
              (remove
                #(= (:id user) (:id %))
                (models/users-find "list_id" (:list_id user))))

        (let [list-url (str "/" (:name (models/list-find "id" (:list_id user))))]
          {:status 302
           :headers {"Location" list-url}
           :cookies {"user-id" {:value (:id user) :path list-url}
                     "auth-token" {:value (:auth_token user) :path list-url}}})))))
