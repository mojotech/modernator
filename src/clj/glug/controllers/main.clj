(ns glug.controllers.main
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [clojure.walk :refer [keywordize-keys]]
            [ring.util.response :as response]
            [postal.core :as postal]
            [glug.views.main :as views]
            [glug.models.main :as models]))

(defn crowd-create [req]
  (let [params (select-keys (keywordize-keys (:form-params req))
                            [:email :crowd :crowd_emails])
        owner (first (models/users-create [{:email (:email params)}]))
        crowd (models/crowd-create
                {:name (:crowd params)
                 :admin_id (:id owner)})]

    (models/user-update {:crowd_id (:id crowd)} ["id=?" (:id owner)])

    (models/users-create
      (mapv #(hash-map :email % :crowd_id (:id crowd))
            (clojure.string/split (:crowd_emails params) #" ")))

    (postal/send-message
      {:from "jake+adam@mojotech.com"
       :to (:email params)
       :subject "Get Started with Glug!"
       :body [{:type "text/html"
               :content (views/admin-welcome-email (:auth_token owner))}]})

    (response/redirect "/signup-confirm")))

(defn crowd-activate [auth-token]
  (let [user (models/user-find "auth_token" auth-token)]
    (if (nil? user)
      (response/response "What the hell?")
      (do
        (models/user-update {:is_verified true} ["id = ?" (:id user)])

        (pmap #(when (not= (:was_invited (models/user-find "id" (:id %))) true)
                 (postal/send-message
                   {:from "jake+adam@mojotech.com"
                    :to (:email %)
                    :subject "You Were Invited to Drink!"
                    :body [{:type "text/html"
                            :content (views/user-welcome-email (:auth_token (:auth_token %)))}]})
                 (models/user-update {:was_invited true} ["id = ?" (:id %)]))
              (remove
                #(= (:id user) (:id %))
                (models/users-find "crowd_id" (:crowd_id user))))

        {:status 302
         :headers {"Location" "/"}
         :cookies {"user-id" {:value (:id user) :path "/"}
                   "auth-token" {:value (:auth_token user) :path "/"}}}))))

(defn user-activate [auth-token]
  (let [user (models/user-find "auth_token" auth-token)]
    (if (nil? user)
      {:status 404}
      (do
        (models/user-update {:is_verified true} ["id = ?" (:id user)])

        {:status 302
         :headers {"Location" "/"}
         :cookies {"user-id" {:value (:id user) :path "/"}
                   "auth-token" {:value (:auth_token user) :path "/"}}}))))

(defn items-index [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        crowd-id (:crowd_id (models/user-find "id" user-id))
        items (models/crowd-items crowd-id)
        votes (models/crowd-votes crowd-id)]
    (response/response
      (map
        #(let [item-votes (filter (fn [vote] (= (:item_id vote) (:id %))) votes)
               user-upvoted? (some
                               (fn [vote] (= (:user_id vote) user-id))
                               item-votes)]
           (assoc % :votes (count item-votes) :upvoted user-upvoted?))
        items))))

(defn items-add [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        crowd-id (:crowd_id (models/user-find "id" user-id))
        title (-> req :body :title)
        item (let [existing-item (models/item-find "title" title)]
               (if-not (nil? existing-item)
                 existing-item
                 (models/item-create!
                   {:crowd_id crowd-id
                    :title title
                    :added_by user-id})))
        existing-vote (models/vote-find crowd-id (:id item) user-id)]
    (if (nil? existing-vote)
      (models/vote-create
        {:crowd_id crowd-id
         :user_id user-id
         :item_id (:id item)}))
    {:status 200}))

(defn vote-toggle [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        item-id (Integer. (get-in req [:params :item_id]))
        crowd-id (:crowd_id (models/user-find "id" user-id))
        user (models/user-find "id" user-id)
        vote (models/vote-find crowd-id item-id user-id)]
    (if (nil? vote)
      (models/vote-create {:crowd_id crowd-id :item_id item-id :user_id user-id})
      (models/vote-delete (:id vote)))
    {:status 200}))

(defroutes main
  (GET "/signup" [] (views/signup))
  (POST "/signup" req (crowd-create req))
  (GET "/signup-confirm" [] (views/signup-confirm))
  (GET "/confirm-crowd/:auth-token" [auth-token] (crowd-activate auth-token))
  (GET "/confirm-user/:auth-token" [auth-token] (user-activate auth-token))

  (GET "/" [] (views/index))
  (GET "/items" req (items-index req))
  (PUT "/items" req (items-add req))
  (PUT "/votes/:item_id" req (vote-toggle req)))
