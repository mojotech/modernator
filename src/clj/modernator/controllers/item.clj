(ns modernator.controllers.item
  (:require [ring.util.response :as response]
            [digest :refer [digest]]
            [clojure.string :as string]
            [modernator.encrypt :refer [encrypt]]
            [modernator.models.main :as models]))

(defn index [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        list-id (:list_id (models/user-find "id" user-id))
        items (models/list-items list-id)
        votes (models/list-votes list-id)]
    (response/response
      (map
        #(let [item-votes (filter (fn [vote] (= (:item_id vote) (:id %))) votes)
               added-by (:added_by %)
               attribution (if added-by
                             (string/capitalize (first (string/split (:email (models/user-find "id" added-by)) #"@")))
                             "Anonymous")
               created-at (.format (java.text.SimpleDateFormat. "M/d/yy @ h:ma") (:created_at %))
               voter-gravatar-hashes (map (fn [voter] (digest/md5 (:email voter))) (models/find-voters (:id %)))
               user-upvoted? (some
                               (fn [vote] (or (= (:user_id vote) user-id)
                                              (= (:user_secret_token vote) (encrypt user-id))))
                               item-votes)]
           (assoc % :votes (count item-votes) :voter-gravatar-hashes voter-gravatar-hashes :created-at created-at :added-by attribution :upvoted user-upvoted?))
        items))))

(defn add [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        list-id (:list_id (models/user-find "id" user-id))
        title (-> req :body :title)
        submitting-anonymously? (-> req :body :submitting_anonymously)
        item (let [existing-item (models/item-find "title" title)]
               (if-not (nil? existing-item)
                 existing-item
                 (models/item-create!
                   (let [vote {:list_id list-id
                               :title title}]
                     (if submitting-anonymously?
                       vote
                       (assoc vote :added_by user-id))))))
        existing-vote (models/vote-find list-id (:id item) user-id)]
    (if (nil? existing-vote)
      (models/vote-create
        {:list_id list-id
         :user_id user-id
         :item_id (:id item)}
        submitting-anonymously?))
    {:status 200}))
