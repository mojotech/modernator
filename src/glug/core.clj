(ns glug.core
  (require [compojure.core :refer [defroutes]]
           [ring.adapter.jetty :as ring]
           [ring.middleware.params :as params]
           [ring.middleware.cookies :as cookies]
           [glug.controllers.main :as controllers])
  (:gen-class))

(defroutes routes
  (-> controllers/main
      controllers/auth
      cookies/wrap-cookies
      params/wrap-params))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))
