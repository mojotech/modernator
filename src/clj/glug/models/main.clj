(ns glug.models.main
  (:require [clojure.java.jdbc :as sql]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.core.cache :as cache]
            [crypto.random :as random]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://localhost:5432/glug"))

(defn users-create [users]
  (apply sql/insert! spec :users (mapv #(assoc % :auth_token (random/url-part 20)) users)))

(defn crowd-create [crowd]
  (first (sql/insert! spec :crowds crowd)))

(defn user-update [set-map where-clause]
  (sql/update! spec :users set-map where-clause))

(defn user-find [column value]
  (first (sql/query spec [(str "select * from users where " column " = ?") value])))

(defn users-find [column value]
  (sql/query spec [(str "select * from users where " column " = ?") value]))

(def untappd-uri "https://api.untappd.com/v4/")

(defn untappd-endpoint [method]
  (str
    untappd-uri
    method
    "?client_id="
    (System/getenv "UNTAPPD_CLIENT_ID")
    "&client_secret="
    (System/getenv "UNTAPPD_CLIENT_SECRET")
    ;(when (not (empty? params))
    ;  "&"
    ;  params)
    ))

(defn crowd-beers [crowd-id]
  (sql/query spec [(str "select * from beers where crowd_id = ?") crowd-id]))

(def beer-cache (cache/ttl-cache-factory {} :ttl (* 1000 60 60 24 365)))

(def parse-req (comp keywordize-keys json/read-str :body deref http/get))

(defn get-untappd-beer [id]
  (let [beer (get-in (parse-req (untappd-endpoint (str "beer/info/" id)))
                     [:response :beer])]
    {:name (:beer_name beer)
     :brewery (get-in beer [:brewery :brewery_name])
     :image (:beer_label beer)}))

(defn beer-find [id]
  (get
    (if (cache/has? beer-cache id)
      (cache/hit beer-cache id)
      (cache/miss beer-cache id
                  (get-untappd-beer id)))
    id))
