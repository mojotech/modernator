(ns glug.controllers.main
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [clojure.walk :refer [keywordize-keys]]
            [ring.util.response :as ring]
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

    (ring/redirect "/signup-confirm")))

(defn crowd-activate [auth-token]
  (let [user (models/user-find "auth_token" auth-token)]
    (if (nil? user)
      (ring/response "What the hell?")
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
      (ring/response "What the hell?")
      (do
        (models/user-update {:is_verified true} ["id = ?" (:id user)])

        {:status 302
         :headers {"Location" "/"}
         :cookies {"user-id" {:value (:id user) :path "/"}
                   "auth-token" {:value (:auth_token user) :path "/"}}}))))

(defn beers-index [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        crowd-id (:crowd_id (models/user-find "id" user-id))
        beers (models/crowd-beers crowd-id)
        votes (models/crowd-votes crowd-id)]
    (ring/response
      (map
        #(let [beer-votes (filter (fn [vote] (= (:beer_id vote) (:id %))) votes)
               user-upvoted? (some (fn [vote] (= (:user_id vote) user-id)) beer-votes)
               beer-info (models/beer-find (:id %))
               ]
           (assoc (merge % beer-info)
                  :votes (count beer-votes) :upvoted user-upvoted?))
        beers))))

(defn beers-search [req]
  (let [query (get-in req [:params :query])
        _ (println req)]
    (ring/response (models/beers-search query))))

(defn beers-add [req]
  (let [untappd-id (Integer. (get-in req [:params :id]))
        user-id (Integer. (:value (get (:cookies req) "user-id")))
        crowd-id (:crowd_id (models/user-find "id" user-id))]
    (models/vote-create
      {:crowd_id crowd-id
       :user_id user-id
       :beer_id (:id (models/beer-create!
                       {:crowd_id crowd-id
                        :untappd_id untappd-id
                        :is_available true
                        :added_by user-id}))})))

(defn vote-toggle [req]
  (let [user-id (Integer. (:value (get (:cookies req) "user-id")))
        beer-id (Integer. (get-in req [:params :beer_id]))
        crowd-id (:crowd_id (models/user-find "id" user-id))
        user (models/user-find "id" user-id)
        vote (models/vote-find crowd-id beer-id user-id)]
    (if (nil? vote)
      (models/vote-create {:crowd_id crowd-id :beer_id beer-id :user_id user-id})
      (models/vote-delete (:id vote)))
    {:status 200}))

(def public-uri-bases #{"/signup" "/confirm-"})

(defn auth [handler]
  (fn [request]
    (let [cookies (:cookies request)
          cookie-auth-token (:value (get cookies "auth-token"))
          cookie-user-id (:value (get cookies "user-id"))]
      (if (or (some #(.contains (:uri request) %) public-uri-bases)
              (and
                (not (nil? cookie-auth-token))
                (not (nil? cookie-user-id))
                (= cookie-auth-token (:auth_token (models/user-find "id" (Integer. cookie-user-id))))))
        (handler request)
        (ring/redirect "/signup")))))

(defroutes main
  (GET "/signup" [] (views/signup))
  (POST "/signup" req (crowd-create req))
  (GET "/signup-confirm" [] (views/signup-confirm))
  (GET "/confirm-crowd/:auth-token" [auth-token] (crowd-activate auth-token))
  (GET "/confirm-user/:auth-token" [auth-token] (user-activate auth-token))

  (GET "/" [] (views/index))
  (GET "/beers" req (beers-index req))
  (PUT "/beers/:id" req (beers-add req))
  (GET "/beers/search/:query" req (beers-search req))
  (PUT "/votes/:beer_id" req (vote-toggle req)))
