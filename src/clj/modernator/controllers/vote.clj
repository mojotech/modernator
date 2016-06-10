(ns modernator.controllers.vote
  (:require [modernator.models.main :as models]))

(defn toggle [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        item-id (Integer. (get-in req [:params :item_id]))
        list-id (:list_id (models/user-find "id" user-id))
        user (models/user-find "id" user-id)
        vote (models/vote-find list-id item-id user-id)
        submitting-anonymously? (-> req :body :submitting_anonymously)]
    (if (nil? vote)
      (models/vote-create {:list_id list-id :item_id item-id :user_id user-id} submitting-anonymously?)
      (models/vote-delete (:id vote)))
    {:status 200}))
