(ns glug.core
  (require [compojure.core :refer [defroutes]]
           [compojure.route :as route]
           [ring.adapter.jetty :as ring]
           [ring.middleware.params :as params]
           [ring.middleware.json :as json]
           [ring.middleware.cookies :as cookies]
           [ring.middleware.transit :refer [wrap-transit-response]]
           [glug.controllers.main :as controllers])
  (:gen-class))

(defroutes routes
  (-> controllers/main
      controllers/auth
      cookies/wrap-cookies
      json/wrap-json-response
      wrap-transit-response
      params/wrap-params)
  (route/resources "/public"))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))
