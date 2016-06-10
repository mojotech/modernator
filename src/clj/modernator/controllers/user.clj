(ns modernator.controllers.user
  (:require [modernator.models.main :as models]))

(defn activate [auth-token]
  (let [user (models/user-find "auth_token" auth-token)]
    (if (nil? user)
      {:status 404}
      (do
        (models/user-update {:is_verified true} ["id = ?" (:id user)])

        (let [list-url (str "/" (:name (models/list-find "id" (:list_id user))))]
          {:status 302
           :headers {"Location" list-url}
           :cookies {"user-id" {:value (:id user) :path list-url}
                     "auth-token" {:value (:auth_token user) :path list-url}}})))))
