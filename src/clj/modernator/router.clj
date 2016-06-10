(ns modernator.router
  (:require [compojure.core :refer [defroutes GET POST PUT]]
            [modernator.views.main :as views]
            [modernator.controllers.main :as controller]))

(defroutes router
  (GET "/signup" [] (views/signup))
  (POST "/signup" req (controller/list-create req))
  (GET "/signup-confirm" [] (views/signup-confirm))
  (GET "/confirm-list/:auth-token" [auth-token] (controller/list-activate auth-token))
  (GET "/confirm-user/:auth-token" [auth-token] (controller/user-activate auth-token))

  (GET "/:list" req (views/index req))
  (GET "/:list/items" req (controller/items-index req))
  (PUT "/:list/items" req (controller/items-add req))
  (PUT "/:list/votes/:item_id" req (controller/vote-toggle req)))
