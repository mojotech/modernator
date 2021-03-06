(ns modernator.router
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [modernator.views.main :as views]
            [modernator.controllers.list :as list]
            [modernator.controllers.user :as user]
            [modernator.controllers.item :as item]
            [modernator.controllers.vote :as vote]))

(defroutes router
  (GET "/signup" [] (views/signup))
  (POST "/signup" req (list/create req))
  (GET "/signup-confirm" [] (views/signup-confirm))
  (GET "/confirm-list/:auth-token" [auth-token] (list/activate auth-token))
  (GET "/confirm-user/:auth-token" [auth-token] (user/activate auth-token))

  (GET "/:list" req (views/index req))
  (GET "/:list/items" req (item/index req))
  (PUT "/:list/items" req (item/add req))
  (PUT "/:list/votes/:item_id" req (vote/toggle req)))
