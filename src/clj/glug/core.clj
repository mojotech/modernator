(ns glug.core
  (require [compojure.core :refer [defroutes]]
           [compojure.route :as route]
           [ring.adapter.jetty :as ring]
           [ring.util.response :as response]
           [ring.middleware.params :as params]
           [ring.middleware.json :as json]
           [ring.middleware.cookies :as cookies]
           [ring.middleware.transit :refer [wrap-transit-response]]
           [glug.models.main :as models]
           [glug.controllers.main :as controllers])
  (:gen-class))

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
        (response/redirect "/signup")))))

(defroutes routes
  (-> controllers/main
      auth
      cookies/wrap-cookies
      json/wrap-json-response
      (json/wrap-json-body {:keywords? true})
      wrap-transit-response
      params/wrap-params)
  (route/resources "/public"))

(defn -main []
  (ring/run-jetty #'routes {:port 8080 :join? false}))
