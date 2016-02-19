(ns modernator.controllers.main
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [clojure.walk :refer [keywordize-keys]]
            [ring.util.response :as response]
            [clj-time.core :as t]
            [digest :refer [digest]]
            [clojure.string :as string]
            [modernator.encrypt :refer [encrypt]]
            [modernator.views.main :as views]
            [modernator.models.mailer :as mailer]
            [modernator.models.main :as models]))

(defn list-create [req]
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

(defn list-activate [auth-token]
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

(defn user-activate [auth-token]
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

(defn items-index [req]
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

(defn items-add [req]
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

(defn vote-toggle [req]
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

(defroutes main
  (GET "/signup" [] (views/signup))
  (POST "/signup" req (list-create req))
  (GET "/signup-confirm" [] (views/signup-confirm))
  (GET "/confirm-list/:auth-token" [auth-token] (list-activate auth-token))
  (GET "/confirm-user/:auth-token" [auth-token] (user-activate auth-token))

  (GET "/:list" req (views/index req))
  (GET "/:list/items" req (items-index req))
  (PUT "/:list/items" req (items-add req))
  (PUT "/:list/votes/:item_id" req (vote-toggle req)))
